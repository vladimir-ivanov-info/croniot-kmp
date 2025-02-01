package com.server.croniot.data.db.daos

import croniot.models.SensorData
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.Transaction
import javax.inject.Inject

class SensorDataDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : SensorDataDao {

    override fun insert(sensorData: SensorData) {
        var session: Session? = null
        var transaction: Transaction? = null
        var taskConfigurationId: Long

        try {
            session = sessionFactory.openSession()
            transaction = session.beginTransaction()

            session.persist(sensorData)
            session.flush()

            transaction.commit()
        } catch (e: java.lang.Exception) {
            println("${sensorData.sensorType} ${sensorData.value}")
            if (transaction != null) {
                transaction.rollback()
            }
            throw e
        } finally {
            if (session != null) {
                session.close()
            }
        }
    }
}
