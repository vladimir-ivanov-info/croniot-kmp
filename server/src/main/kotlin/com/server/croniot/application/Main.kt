package com.server.croniot.application

import Global
import com.server.croniot.di.DI
import com.server.croniot.mqtt.MqttController
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import croniot.messages.MessageFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.event.Level
import java.io.File
import java.security.KeyStore
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

fun Application.module(testing: Boolean = false) {
    Global.TESTING = testing
    logger.info { "Testing mode: ${Global.TESTING}" }

    install(ContentNegotiation) {
        json(MessageFactory.json)
    }

    install(CallLogging) {
        level = Level.INFO
    }

    val appComponent = DI.appComponent
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

fun provideDataSource(): DataSource { //TODO duplicate for now
    val secrets = Global.secrets
    val config = HikariConfig().apply {
        jdbcUrl = secrets.databaseUrl
        username = secrets.databaseUser
        password = secrets.databasePassword

        maximumPoolSize = 8
        minimumIdle = 2
        idleTimeout = 30_000
        maxLifetime = 30 * 60_000
        connectionTimeout = 10_000

        poolName = "croniot-hikari"
    }

    return HikariDataSource(config)
}

fun main() {
    ensureDockerComposeRunning()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            logger.info { "Gracefully shutting down..." }
        },
    )

    initDatabase(provideDataSource())

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
                    //port = 8090 //port = 8443
                    port = 8443
                }
            },
            module = { module() }
        ).start(wait = true)
    } catch (e: Throwable) {
        logger.error(e) { "Fatal error during server startup" }
    }
}
