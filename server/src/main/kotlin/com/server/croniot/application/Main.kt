package com.server.croniot.application

import Global
import com.server.croniot.di.DI
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageFactory
import croniot.models.errors.DomainError
import croniot.models.errors.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.ktor.server.response.respond
import org.slf4j.event.Level
import java.io.File
import java.security.KeyStore
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

const val AUTH_JWT_REALM = "auth-jwt"
const val MDC_TRACE_ID = "traceId"
const val HEADER_TRACE_ID = "X-Trace-Id"
val RATE_LIMIT_AUTH: RateLimitName = RateLimitName("auth")
val PROMETHEUS_REGISTRY_KEY: io.ktor.util.AttributeKey<PrometheusMeterRegistry> =
    io.ktor.util.AttributeKey("PrometheusMeterRegistry")

fun Application.module(testing: Boolean = false) {
    Global.TESTING = testing
    logger.info { "Testing mode: ${Global.TESTING}" }

    install(ContentNegotiation) {
        json(MessageFactory.json)
    }

    install(CallLogging) {
        level = Level.INFO
        mdc(MDC_TRACE_ID) { call ->
            call.request.headers[HEADER_TRACE_ID]?.takeIf { it.isNotBlank() }
                ?: java.util.UUID.randomUUID().toString()
        }
    }

    install(RateLimit) {
        register(RATE_LIMIT_AUTH) {
            rateLimiter(limit = 5, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }

    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmThreadMetrics(),
            ProcessorMetrics(),
        )
    }
    attributes.put(PROMETHEUS_REGISTRY_KEY, prometheusRegistry)

    installStatusPages()

    val appComponent = DI.appComponent
    val jwtConfig = appComponent.jwtConfig()

    install(Authentication) {
        jwt(AUTH_JWT_REALM) {
            realm = "croniot"
            verifier(jwtConfig.verifier())
            validate { credential ->
                val subject = credential.payload.subject
                val hasAudience = credential.payload.audience.contains(jwtConfig.audience)
                if (!subject.isNullOrBlank() && hasAudience) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(
                        code = DomainError.Unauthorized.CODE,
                        message = "Invalid or expired token",
                    ),
                )
            }
        }
    }

    val routeInitializer = RouteInitializer(
        loginController = appComponent.loginController(),
        accountController = appComponent.accountController(),
        deviceController = appComponent.deviceController(),
        taskController = appComponent.taskController(),
        sensorTypeController = appComponent.sensorTypeController(),
        taskTypeController = appComponent.taskTypeController(),
    )
    routeInitializer.setupRoutes(this)
}

fun ensureDockerComposeRunning() {
    val composeDir = File(System.getProperty("user.dir"))

    val checkProcess = ProcessBuilder("docker", "compose", "ps", "--status", "running", "-q")
        .directory(composeDir)
        .redirectErrorStream(true)
        .start()

    val running = checkProcess.inputStream.bufferedReader().readText().trim()
    checkProcess.waitFor()

    if (running.isEmpty()) {
        logger.info { "Docker Compose not running, starting services..." }

        val upProcess = ProcessBuilder("docker", "compose", "up", "-d")
            .directory(composeDir)
            .inheritIO()
            .start()

        val exitCode = upProcess.waitFor()
        if (exitCode != 0) {
            error("Error al levantar Docker Compose (exit code: $exitCode)")
        }

        logger.info { "Waiting 5s for services to be ready..." }
        Thread.sleep(5000)
        logger.info { "Docker Compose services started" }
    } else {
        logger.info { "Docker Compose already running" }
    }
}

fun initDatabase(dataSource: DataSource) {
    DatabaseSchemaInitializer.createSchemaIfNeeded(dataSource)
}

fun main() {
    ensureDockerComposeRunning()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Gracefully shutting down..." }
            runCatching { DI.appComponent.applicationScope().shutdown() }
                .onFailure { logger.warn(it) { "Error cancelling ApplicationScope" } }
            runCatching { MqttController.shutdown() }
                .onFailure { logger.warn(it) { "Error cancelling MqttController scope" } }
        },
    )

    initDatabase(DI.appComponent.dataSource())

    try {
        MqttController
        DI.appComponent.sensorDataController().start()

        val keystorePassword = Global.secrets.keystorePassword

        val keyStore = KeyStore.getInstance("PKCS12").apply {
            File("croniot-keystore.p12").inputStream().use {
                load(it, keystorePassword.toCharArray())
            }
        }

        embeddedServer(
            Netty,
            environment = applicationEnvironment { },
            configure = {
                connector {
                    host = "0.0.0.0"
                    port = 8090
                }
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = "croniot",
                    keyStorePassword = { keystorePassword.toCharArray() },
                    privateKeyPassword = { keystorePassword.toCharArray() }
                ) {
                    host = "0.0.0.0"
                    // port = 8090 //port = 8443
                    port = 8443
                }
            },
            module = { module() }
        ).start(wait = true)
    } catch (e: Throwable) {
        logger.error(e) { "Fatal error during server startup" }
    }
}
