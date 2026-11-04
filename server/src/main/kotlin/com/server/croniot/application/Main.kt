package com.server.croniot.application

import Global
import com.server.croniot.di.DI
import com.server.croniot.livelog.ServerLogDbWriter
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.sse.SSE
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.event.Level
import java.io.File
import java.security.KeyStore
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
    // Lets services that are not handed the registry (e.g.
    // IngestBatchRecorder) register counters that still show up in /metrics.
    Metrics.addRegistry(prometheusRegistry)

    installStatusPages()

    install(SSE)

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
            challenge { _, _ -> call.respondInvalidToken() }
        }
    }

    val routeInitializer = RouteInitializer(
        loginController = appComponent.loginController(),
        accountController = appComponent.accountController(),
        deviceController = appComponent.deviceController(),
        taskController = appComponent.taskController(),
        sensorTypeController = appComponent.sensorTypeController(),
        taskTypeController = appComponent.taskTypeController(),
        featureFlagController = appComponent.featureFlagController(),
        deviceLogConfigController = appComponent.deviceLogConfigController(),
        deviceLogController = appComponent.deviceLogController(),
        adminLogStreamController = appComponent.adminLogStreamController(),
        adminLogController = appComponent.adminLogController(),
        adminDirectoryController = appComponent.adminDirectoryController(),
        adminServerLogLevelController = appComponent.adminServerLogLevelController(),
        otaController = appComponent.otaController(),
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

        logger.info { "Waiting for Postgres to be ready..." }
        waitForPostgresReady(composeDir)
        logger.info { "Docker Compose services started" }
    } else {
        logger.info { "Docker Compose already running" }
    }
}

private fun waitForPostgresReady(
    composeDir: File,
    timeout: Duration = 30.seconds,
    pollInterval: Duration = 1.seconds,
) {
    val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds

    while (System.currentTimeMillis() < deadline) {
        val readyProcess = ProcessBuilder("docker", "compose", "exec", "-T", "db", "pg_isready", "-U", "testuser", "-d", "iot_testdb")
            .directory(composeDir)
            .redirectErrorStream(true)
            .start()

        val exitCode = readyProcess.waitFor()
        if (exitCode == 0) {
            return
        }

        Thread.sleep(pollInterval.inWholeMilliseconds)
    }

    error("Postgres no respondió como listo tras ${timeout.inWholeSeconds}s")
}

fun initDatabase(dataSource: DataSource) {
    DatabaseMigrator.migrate(dataSource)
}

fun main() {
    ensureDockerComposeRunning()
    AdminUiLauncher.start()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Gracefully shutting down..." }
            runCatching { AdminUiLauncher.stop() }
                .onFailure { logger.warn(it) { "Error stopping Admin UI" } }
            runCatching { DI.appComponent.applicationScope().shutdown() }
                .onFailure { logger.warn(it) { "Error cancelling ApplicationScope" } }
            runCatching { MqttController.shutdown() }
                .onFailure { logger.warn(it) { "Error cancelling MqttController scope" } }
        },
    )

    initDatabase(DI.appComponent.dataSource())

    // After initDatabase (server_log must exist). Lines logged before this
    // wait in ServerLogQueue and are written on the first flush.
    val serverLogWriter = ServerLogDbWriter(DI.appComponent.dslContext()).also { it.start() }
    Runtime.getRuntime().addShutdownHook(Thread { runCatching { serverLogWriter.stop() } })

    DI.appComponent.logRetentionService().schedule(DI.appComponent.applicationScope())

    try {
        MqttController.start()
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
