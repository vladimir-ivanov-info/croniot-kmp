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
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyStore
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
        featureFlagController = appComponent.featureFlagController(),
        deviceLogConfigController = appComponent.deviceLogConfigController(),
        deviceLogController = appComponent.deviceLogController(),
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

private const val ADMIN_UI_PORT = 5173
private const val ADMIN_UI_URL = "http://localhost:$ADMIN_UI_PORT"

private var adminUiProcess: Process? = null

fun ensureAdminUiRunning() {
    if (System.getenv("CRONIOT_ADMIN_UI").equals("false", ignoreCase = true)) {
        logger.info { "Admin UI auto-start disabled (CRONIOT_ADMIN_UI=false)" }
        return
    }

    val adminUiDir = System.getenv("CRONIOT_ADMIN_UI_DIR")?.let(::File)
        ?: File(System.getProperty("user.dir"), "../admin-ui")
    if (!File(adminUiDir, "package.json").isFile) {
        logger.warn { "Admin UI not found at ${adminUiDir.canonicalPath}, skipping" }
        return
    }

    if (isLocalPortInUse(ADMIN_UI_PORT)) {
        logger.info { "Admin UI already running: $ADMIN_UI_URL" }
        return
    }

    val npm = System.getenv("NPM_PATH") ?: "npm"
    val logFile = File(System.getProperty("user.dir"), "logs/admin-ui.log").apply { parentFile.mkdirs() }

    try {
        if (!File(adminUiDir, "node_modules").isDirectory) {
            logger.info { "Installing Admin UI dependencies (npm ci)..." }
            val exitCode = ProcessBuilder(npm, "ci")
                .directory(adminUiDir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                .start()
                .waitFor()
            if (exitCode != 0) {
                logger.warn { "npm ci failed (exit code: $exitCode), see ${logFile.path}" }
                return
            }
        }

        val process = ProcessBuilder(npm, "run", "dev", "--", "--port", "$ADMIN_UI_PORT", "--strictPort")
            .directory(adminUiDir)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
            .start()
        adminUiProcess = process
        logger.info { "Starting Admin UI (logs: ${logFile.path})" }
        logWhenAdminUiReady(process, logFile)
    } catch (e: IOException) {
        logger.warn(e) { "Could not start Admin UI (is npm on PATH? otherwise set NPM_PATH)" }
    }
}

// Runs in the background so the server boot isn't blocked waiting for vite
private fun logWhenAdminUiReady(
    process: Process,
    logFile: File,
    timeout: Duration = 60.seconds,
    pollInterval: Duration = 500.milliseconds,
) = thread(isDaemon = true, name = "admin-ui-readiness") {
    val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds

    while (System.currentTimeMillis() < deadline) {
        if (!process.isAlive) {
            logger.warn { "Admin UI exited (exit code: ${process.exitValue()}), see ${logFile.path}" }
            return@thread
        }
        if (isLocalPortInUse(ADMIN_UI_PORT)) {
            logger.info { "Admin UI ready: $ADMIN_UI_URL" }
            return@thread
        }
        Thread.sleep(pollInterval.inWholeMilliseconds)
    }

    logger.warn { "Admin UI not ready after ${timeout.inWholeSeconds}s, see ${logFile.path}" }
}

fun stopAdminUi() {
    // npm spawns vite as a child process; destroying only npm would leave vite holding the port
    adminUiProcess?.let { process ->
        process.descendants().forEach { it.destroy() }
        process.destroy()
    }
}

private fun isLocalPortInUse(port: Int): Boolean =
    // vite may bind to ::1 or 127.0.0.1 depending on how "localhost" resolves
    InetAddress.getAllByName("localhost").any { address ->
        runCatching { Socket().use { it.connect(InetSocketAddress(address, port), 200) } }.isSuccess
    }

fun initDatabase(dataSource: DataSource) {
    DatabaseSchemaInitializer.createSchemaIfNeeded(dataSource)
}

fun main() {
    ensureDockerComposeRunning()
    ensureAdminUiRunning()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Gracefully shutting down..." }
            runCatching { stopAdminUi() }
                .onFailure { logger.warn(it) { "Error stopping Admin UI" } }
            runCatching { DI.appComponent.applicationScope().shutdown() }
                .onFailure { logger.warn(it) { "Error cancelling ApplicationScope" } }
            runCatching { MqttController.shutdown() }
                .onFailure { logger.warn(it) { "Error cancelling MqttController scope" } }
        },
    )

    initDatabase(DI.appComponent.dataSource())

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
