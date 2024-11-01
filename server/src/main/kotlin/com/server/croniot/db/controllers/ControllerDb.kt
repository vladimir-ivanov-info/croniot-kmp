package com.croniot.server.db.controllers

import Global
import TaskController
import com.croniot.server.db.daos.*
import db.daos.*
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.Persistence


object ControllerDb {

    lateinit var sessionFactory: SessionFactory

    fun getConnection() : Connection{
        return connection;
    }

    private val connection: Connection = DriverManager.getConnection(
        Global.secrets.databaseUrl,
        Global.secrets.databaseUser,
        Global.secrets.databasePassword
    )

    @Throws(Throwable::class)
    fun initialize(){
        var configuration : Configuration

        if(Global.TESTING){
            configuration = Configuration().configure("META-INF/hibernate-test.cfg.xml") // Load hibernate.cfg.xml file
        } else {
            configuration = Configuration().configure("META-INF/hibernate.cfg.xml") // Load hibernate.cfg.xml file
        }

        sessionFactory = configuration.buildSessionFactory()
        initDaos()

        TaskController
    }

    lateinit var accountDao : AccountDao
    lateinit var deviceDao : DeviceDao
    lateinit var sensorDao : SensorDao
    lateinit var taskTypeDao : TaskTypeDao
    lateinit var taskDao : TaskDao
    lateinit var taskStateInfoDao: TaskStateInfoDao
    lateinit var parameterTaskDao : ParameterTaskDao
    lateinit var deviceTokenDao : DeviceTokenDao

    lateinit var sensorDataDao : SensorDataDao

    fun initDaos(){
        accountDao = AccountDaoImpl()
        deviceDao = DeviceDaoImpl()
        sensorDao = SensorDaoImpl()
        taskTypeDao = TaskTypeDaoImpl()
        taskDao = TaskDaoImpl()
        taskStateInfoDao = TaskStateInfoDaoImpl()
        parameterTaskDao = ParameterTaskDaoImpl()
        deviceTokenDao = DeviceTokenDaoImpl()

        sensorDataDao = SensorDataDaoImpl()

    }

    fun removeAllTables(){
        //removeTableClient()
        //removeTableAccountDevice()
        //removeTableAccount()
        deleteAllDataFromTable("device_token")

        deleteAllDataFromTable("parameter_sensor_constraints")
        deleteAllDataFromTable("parameter_task_constraints")

        deleteAllDataFromTable("parameter_sensor")
        deleteAllDataFromTable("sensor")
        deleteAllDataFromTable("parameter_task")
        //deleteAllDataFromTable("parameter")
        deleteAllDataFromTable("task")
        //deleteAllDataFromTable("parameter_constraints")
        deleteAllDataFromTable("device")
        deleteAllDataFromTable("account")
        //deleteAllDataFromTable("device")
        //deleteAllDataFromTable("sensor")

      //  removeTableDeviceToken()
      //  removeTableSensorInfo()
      //  removeTableSensorInfoConstraint()

        //removeTableTaskInfo()
        //removeTableTaskInfoParameter()
        //removeTableTaskInfoParameterConstraint()
      // println()
    }

    fun removeTableClient(){
        val sqlCreate = "DROP TABLE IF EXISTS device"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    fun removeTableAccountDevice(){
        val sqlCreate = "DROP TABLE IF EXISTS account_device"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
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



    fun deleteAllDataFromTable(tableName: String) {
        var entityManagerFactory: EntityManagerFactory? = null
        var entityManager: EntityManager? = null
        var transaction: EntityTransaction? = null
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory("iot-unit")
            entityManager = entityManagerFactory.createEntityManager()
            transaction = entityManager.transaction
            transaction.begin()

            // Execute a native SQL DELETE query to remove all rows from the table
            entityManager.createNativeQuery("DELETE FROM $tableName").executeUpdate()
            transaction.commit()
        } catch (e: java.lang.RuntimeException) {
            if (transaction != null && transaction.isActive) {
                transaction.rollback()
            }
            throw e
        } finally {
            entityManager?.close()
            entityManagerFactory?.close()
        }
    }

//    fun removeTableAccount(){
//        /*val sqlCreate = "DROP TABLE IF EXISTS account"
//        var stmt = connection.createStatement()
//        stmt.execute(sqlCreate)
//        */
//
//        deleteAllDataFromTable("account")
//    }

    fun removeTableDeviceToken(){
        val sqlCreate = "DROP TABLE IF EXISTS device_token"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    fun removeTableSensorInfo(){
        val sqlCreate = "DROP TABLE IF EXISTS sensor_info"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    fun removeTableSensorInfoConstraint(){
        val sqlCreate = "DROP TABLE IF EXISTS sensor_info_constraint"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    fun removeTableTaskInfo(){
        val sqlCreate = "DROP TABLE IF EXISTS task_info"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    fun removeTableTaskInfoParameter(){
        val sqlCreate = "DROP TABLE IF EXISTS task_info_parameter"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    fun removeTableTaskInfoParameterConstraint(){
        val sqlCreate = "DROP TABLE IF EXISTS task_info_parameter_constraint"
        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }



    @Throws(SQLException::class)
    fun addMockData() { //TODO add field for SO: Windows, Web, Android, ...

        //1. Account
        var sqlCreate = ("INSERT INTO account (id, email, password)"
                + " VALUES ('0', 'email123@gmail.com', 'password123');")

        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        //2. Client
     //   sqlCreate = ("INSERT INTO client (id, uuid, password)"
     //           + " VALUES ('0', 'esp32id', 'esp32password');")

        sqlCreate = ("INSERT INTO client (uuid, password, name)"
                + " VALUES ('esp32id', 'esp32password', 'Smart watering system');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        sqlCreate = ("INSERT INTO client (uuid, password, name)"
                + " VALUES ('androidId', 'androidPassword', 'Vladimir Android device');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        //3. AccountClient

        sqlCreate = ("INSERT INTO account_device (id_account, id_client, account_status)"
                + " VALUES ('1', '1', 'main');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        sqlCreate = ("INSERT INTO account_device (id_account, id_client, account_status)"
                + " VALUES ('1', '2', 'main');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        //3.
        /*sqlCreate = ("INSERT INTO sensor_info (id_client, id_sensor, name, type, unit, description)"
                + " VALUES ('1', '1', 'CPU temperature', 'number', 'ºC', 'Chip temperature in ºC');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)*/

        sqlCreate = ("INSERT INTO sensor_info (id_device, id_sensor, name, type, unit, description)"
                + " VALUES ('1', '2', 'WiFi strength', 'number', 'dBm', 'WiFi signal strength expressed in dBm');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        sqlCreate = ("INSERT INTO sensor_info_constraint (id_device, id_sensor, name, value)"
                + " VALUES ('1', '2', 'maxValue', '0');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        sqlCreate = ("INSERT INTO sensor_info_constraint (id_device, id_sensor, name, value)"
                + " VALUES ('1', '2', 'minValue', '-100');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        //3
        sqlCreate = ("INSERT INTO sensor_info (id_device, id_sensor, name, type, unit, description)"
                + " VALUES ('1', '3', 'Battery voltage', 'number', 'V', 'Battery voltage');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        sqlCreate = ("INSERT INTO sensor_info_constraint (id_device, id_sensor, name, value)"
                + " VALUES ('1', '3', 'minValue', '10.0');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

        sqlCreate = ("INSERT INTO sensor_info_constraint (id_device, id_sensor, name, value)"
                + " VALUES ('1', '3', 'maxValue', '13.2');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)


        //addMockTaskInfo()


    }

    fun addMockTaskInfo(){
        var sqlCreate = ("INSERT INTO task_info (id, id_device, name, description)"
                + " VALUES ('1', '1', 'Water plants', 'Task that waters the plants');")

        var stmt = connection.createStatement()
        stmt.execute(sqlCreate)


        sqlCreate = ("INSERT INTO task_info_parameter (id, id_device, id_task_info, name, type, unit, description)"
                + " VALUES ('1', '1', '1', 'minutes', 'number', 'Number of minutes that the plants will be watered.');")

        stmt = connection.createStatement()
        stmt.execute(sqlCreate)

    }

    @Throws(SQLException::class)
    fun createTablesIfNotExist() {

        createAccountTable()
        //createDeviceTable();
        createDeviceTable()
        createAccountDeviceTable()
        //createAccountClientTable()

        createDeviceTokenTable()

        createSensorInfoTable()
        createClientSensorInfoConstraintTable()
        
        //createSensorInfoTable()
        createSensorDataTable()

        //createTaskInfoTable()
        //createTaskInfoParameterTable()
        //createTaskInfoParameterConstraintTable()
    }

    @Throws(SQLException::class)
    fun createAccountTable() { //TODO add field for SO: Windows, Web, Android, ...
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "account"
//                + "  (id int unsigned not null auto_increment,"
//                + "   uuid varchar(50) not null,"
//                + "   email varchar(50) not null,"
//                + "   password varchar(50) not null,"
//                + "PRIMARY KEY (id, email)"
//                + ")")
//        val stmt = connection.createStatement()
//        stmt.execute(sqlCreate)
        val sqlCreate = """
        CREATE TABLE IF NOT EXISTS account (
            id SERIAL,
            uuid VARCHAR(50) NOT NULL,
            email VARCHAR(50) NOT NULL,
            password VARCHAR(50) NOT NULL,
            PRIMARY KEY (id, email)
        )
    """.trimIndent()

        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)




    }

    @Throws(SQLException::class)
    fun createDeviceTable() { //TODO add field for SO: Windows, Web, Android, ...
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "device"
//                + "  (id int unsigned not null auto_increment,"
//                + "   uuid varchar(50) not null,"
//                + "   password varchar(50) not null,"
//                + "   name varchar(50) not null,"
//                + "   PRIMARY KEY (id, uuid)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS device (
        id SERIAL,
        uuid VARCHAR(50) NOT NULL,
        password VARCHAR(50) NOT NULL,
        name VARCHAR(50) NOT NULL,
        PRIMARY KEY (id, uuid)
    )
""".trimIndent()

       /* val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "client"
                + "   uuid varchar(50) not null auto_increment primary key,"
                + "   password varchar(50) not null"
                + ")")*/

        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    @Throws(SQLException::class)
    fun createAccountDeviceTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "account_device"
//                + "  (id_account int unsigned not null,"
//                + "   id_device varchar(50) not null,"
//                + "   account_status varchar(50) not null"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS account_device (
        id_account SERIAL,
        id_device VARCHAR(50) NOT NULL,
        account_status VARCHAR(50) NOT NULL,
        PRIMARY KEY (id_account, id_device)
    )
""".trimIndent()

        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    @Throws(SQLException::class)
    fun createDeviceTokenTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "device_token"
//                + "  (id_device int unsigned not null,"
//                + "   token varchar(50) not null,"
//                + "   PRIMARY KEY (id_device, token)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS device_token (
        id_device SERIAL,
        token VARCHAR(50) NOT NULL,
        PRIMARY KEY (id_device, token)
    )
""".trimIndent()
        /* val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "client"
                 + "   uuid varchar(50) not null auto_increment primary key,"
                 + "   password varchar(50) not null"
                 + ")")*/

        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }
    @Throws(SQLException::class)
    fun createSensorInfoTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "sensor_info"
//                + "   (id_device int unsigned not null,"
//                + "   id_sensor int unsigned not null,"
//                + "   name varchar(50) not null,"
//                + "   type varchar(50) not null,"
//                + "   unit varchar(50) not null,"
//                + "   description varchar(500) not null,"
//                + "   PRIMARY KEY (id_device, id_sensor)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS sensor_info (
        id_device SERIAL,
        id_sensor SERIAL,
        name VARCHAR(50) NOT NULL,
        type VARCHAR(50) NOT NULL,
        unit VARCHAR(50) NOT NULL,
        description VARCHAR(500) NOT NULL,
        PRIMARY KEY (id_device, id_sensor)
    )
""".trimIndent()
        /* val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "client"
                 + "   uuid varchar(50) not null auto_increment primary key,"
                 + "   password varchar(50) not null"
                 + ")")*/

        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    @Throws(SQLException::class)
    fun createClientSensorInfoConstraintTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "sensor_info_constraint"
//                + "   (id_device int unsigned not null,"
//                + "   id_sensor int unsigned not null,"
//                + "   name varchar(50) not null,"
//                + "   value varchar(50) not null,"
//                + "   PRIMARY KEY (id_device, id_sensor, name, value)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS sensor_info_constraint (
        id_device SERIAL,
        id_sensor SERIAL,
        name VARCHAR(50) NOT NULL,
        value VARCHAR(50) NOT NULL,
        PRIMARY KEY (id_device, id_sensor, name, value)
    )
""".trimIndent()
        /* val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "client"
                 + "   uuid varchar(50) not null auto_increment primary key,"
                 + "   password varchar(50) not null"
                 + ")")*/

        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }


    
    @Throws(SQLException::class)
    fun createSensorDataTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "sensor_data"
//                + "  (id_client varchar(50) not null,"
//                + "   id_sensor int unsigned not null,"
//                + "   value varchar(50) not null,"
//                + "   datetime DATETIME not null,"
//                + "   PRIMARY KEY (id_client, id_sensor, value, datetime)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS sensor_data (
        id_client VARCHAR(50) NOT NULL,
        id_sensor SERIAL,
        value VARCHAR(50) NOT NULL,
        datetime TIMESTAMP NOT NULL,
        PRIMARY KEY (id_client, id_sensor, value, datetime)
    )
""".trimIndent()
        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    @Throws(SQLException::class)
    fun createTaskInfoTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "task_info"
//                + "  (id int unsigned not null,"
//                + "   id_device int unsigned not null,"
//                + "   name varchar(1024) not null,"
//                + "   description varchar(1024),"
//                + "   PRIMARY KEY (id, id_device)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS task_info (
        id SERIAL PRIMARY KEY,
        id_device SERIAL,
        name VARCHAR(1024) NOT NULL,
        description VARCHAR(1024)
    )
""".trimIndent()
        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

    @Throws(SQLException::class)
    fun createTaskInfoParameterTable(){
/*        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "task_info_parameter"
                + "  (id int unsigned not null,"
                + "   id_device int unsigned not null,"
                + "   id_task_info int unsigned not null,"
                + "   name varchar(50) not null,"
                + "   type varchar(50) not null,"
                + "   unit varchar(50) not null,"
                + "   description varchar(50) not null,"
                + "   PRIMARY KEY (id, id_device, id_task_info)"
                + ")")*/
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS task_info_parameter (
        id SERIAL PRIMARY KEY,
        id_device SERIAL,
        id_task_info SERIAL,
        name VARCHAR(50) NOT NULL,
        type VARCHAR(50) NOT NULL,
        unit VARCHAR(50) NOT NULL,
        description VARCHAR(50) NOT NULL
    )
""".trimIndent()
        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }
    @Throws(SQLException::class)
    fun createTaskInfoParameterConstraintTable(){
//        val sqlCreate = ("CREATE TABLE IF NOT EXISTS " + "task_info_parameter_constraint"
//                + "  (id int unsigned not null,"
//                + "   id_device int unsigned not null,"
//                + "   id_task_info int unsigned not null,"
//                + "   id_task_info_parameter int unsigned not null,"
//                + "   name varchar(50) not null,"
//                + "   value varchar(50) not null,"
//                + "   PRIMARY KEY (id, id_device, id_task_info, id_task_info_parameter)"
//                + ")")
        val sqlCreate = """
    CREATE TABLE IF NOT EXISTS task_info_parameter_constraint (
        id SERIAL PRIMARY KEY,
        id_device SERIAL,
        id_task_info SERIAL,
        id_task_info_parameter SERIAL,
        name VARCHAR(50) NOT NULL,
        value VARCHAR(50) NOT NULL
    )
""".trimIndent()
        val stmt = connection.createStatement()
        stmt.execute(sqlCreate)
    }

}