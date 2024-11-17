package com.croniot.server.db.daos

import com.croniot.server.db.controllers.ControllerDb
import croniot.models.SensorData
import org.hibernate.Session
import org.hibernate.Transaction
import java.sql.Connection

class SensorDataDaoImpl : SensorDataDao {

    override fun insert(sensorData: SensorData){
        var session: Session? = null
        var transaction: Transaction? = null
        var taskConfigurationId: Long

        try {
            session =
                ControllerDb.sessionFactory.openSession() // Open a new session for this thread
            transaction = session.beginTransaction() // Start a transaction

            session.persist(sensorData) // Persist the task
            session.flush() // Ensure the task is inserted into the database


            transaction.commit() // Commit the transaction
        } catch (e: java.lang.Exception) {
            println("${sensorData.sensorType} ${sensorData.value}")
            if (transaction != null) {
                transaction.rollback() // Rollback on exception
            }
            throw e // Rethrow the exception
        } finally {
            if (session != null) {
                session.close() // Always close the session
            }
        }
    }

}