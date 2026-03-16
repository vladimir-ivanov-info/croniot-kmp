package com.server.croniot.application

import Global
import com.server.croniot.di.DI
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageFactory
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import java.io.File
import java.security.KeyStore

fun Application.module(testing: Boolean = false) {
    Global.TESTING = testing
    print("TESTING: ${Global.TESTING}")

    install(ContentNegotiation) {
        json(MessageFactory.json)
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
        println("Docker Compose no esta activo. Levantando servicios...")

        val upProcess = ProcessBuilder("docker", "compose", "up", "-d")
            .directory(composeDir)
            .inheritIO()
            .start()

        val exitCode = upProcess.waitFor()
        if (exitCode != 0) {
            error("Error al levantar Docker Compose (exit code: $exitCode)")
        }

        println("Esperando 5 segundos a que los servicios esten listos...")
        Thread.sleep(5000)
        println("Servicios levantados")
    } else {
        println("Docker Compose ya esta activo")
    }
}

fun main() {
    ensureDockerComposeRunning()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("Gracefully shutting down...")
        },
    )

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
                    port = 8443
                }
            },
            module = { module() }
        ).start(wait = true)
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}
