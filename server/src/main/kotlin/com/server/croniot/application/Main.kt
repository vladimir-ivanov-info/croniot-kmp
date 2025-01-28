package com.server.croniot.application

import Global
import com.server.croniot.mqtt.MqttController
import ZonedDateTimeAdapter
import com.croniot.server.db.controllers.ControllerDb
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import java.time.ZonedDateTime

fun Application.module(testing: Boolean = false) {
    Global.TESTING = testing
    print("TESTING: ${Global.TESTING}")

    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        }
    }

    configureRouting()
}


fun main() {
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Gracefully shutting down...")
    })

    try{
        //val appComponent: AppComponent = DaggerAppComponent.create()
        //appComponent.inject()

        ControllerDb.initialize()
        MqttController

        embeddedServer(
            Netty,
            port = 8090,
            host = "0.0.0.0",
            module = Application::module
            //module = Application::module).start(wait = true)
            //module = Application::configureRouting
        ).start(wait = true)

        readLine()
    } catch (e: Throwable){
        e.printStackTrace()
    }
}

/*

SELECT * FROM public.account
SELECT * FROM public.device
SELECT * FROM public.sensor
SELECT * FROM public.task
SELECT * FROM public.parameter_sensor
SELECT * FROM public.parameter_task
SELECT * FROM public.parameter_sensor_constraints
SELECT * FROM public.parameter_task_constraints
SELECT * FROM public.device_token


DROP TABLE public.parameter_sensor_constraints CASCADE;
DROP TABLE public.parameter_task_constraints CASCADE;
DROP TABLE public.parameter_sensor CASCADE;
DROP TABLE public.parameter_task CASCADE;
DROP TABLE public.sensor CASCADE;
DROP TABLE public.task_state_info CASCADE;
DROP TABLE public.task_parameter_value CASCADE;
DROP TABLE public.task CASCADE;
DROP TABLE public.task_type CASCADE;
DROP TABLE public.device CASCADE;
DROP TABLE public.account CASCADE;
DROP TABLE public.device_token CASCADE;
*/
