package com.croniot.server.db.controllers

import Global
//import com.server.croniot.http.TaskController
import com.croniot.server.db.daos.*
import com.server.croniot.application.AppComponent
import com.server.croniot.application.DaggerAppComponent
import com.server.croniot.controllers.TaskController
import com.server.croniot.http.SensorsDataController
import com.server.croniot.services.DeviceService
import com.server.croniot.services.TaskService
import com.server.croniot.services.TaskTypeService
import db.daos.*
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import javax.inject.Inject
/*
class ControllerDb @Inject constructor(
    private val taskController: TaskController
)*/

//TODO convert to dagger
object ControllerDb
{

    lateinit var sessionFactory: SessionFactory

  /*  var sessionFactory by lazy {

    } //SessionFactory*/

    fun getConnection() : Connection{
        return connection;
    }

    private val connection: Connection = DriverManager.getConnection(
        Global.secrets.databaseUrl,
        Global.secrets.databaseUser,
        Global.secrets.databasePassword
    )

    //TODO convert sessionFactory in "by lazy"
    @Throws(Throwable::class)
    fun initialize(){
        var configuration : Configuration

        if(Global.TESTING){
            configuration = Configuration().configure("META-INF/hibernate-test.cfg.xml") // Load hibernate.cfg.xml file
        } else {
            configuration = Configuration().configure("META-INF/hibernate.cfg.xml") // Load hibernate.cfg.xml file
        }

        sessionFactory = configuration.buildSessionFactory()

        val appComponent: AppComponent = DaggerAppComponent.create()

       // appComponent.taskController().initMqtt()
        appComponent.sensorDataController().start() // TODO
    }

    private fun buildSessionFactory(): SessionFactory {
        return try {
            // Create the StandardServiceRegistry
            val standardRegistry = StandardServiceRegistryBuilder()
                .configure() // Load configuration from hibernate.cfg.xml
                .build()

            // Create MetadataSources
            val metadataSources = MetadataSources(standardRegistry)

            // Build Metadata
            metadataSources.buildMetadata().buildSessionFactory()
        } catch (ex: Throwable) {
            // Make sure you log the exception
            throw ExceptionInInitializerError(ex)
        }
    }

}