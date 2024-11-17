package com.croniot.server.db.daos

import com.croniot.server.db.controllers.ControllerDb
import croniot.models.Device
import croniot.models.SensorType
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

class SensorDaoImpl : SensorDao {

    override fun insert(sensorType: SensorType) : Long {
        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val sensorId: Long
        try {
            session.persist(sensorType)
            session.flush()
            sensorId = sensorType.id

            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }
        return sensorId
    }

    override fun getLazy(deviceUuid: String, sensorTypeUid: Long): SensorType? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb: CriteriaBuilder = sess.criteriaBuilder
            val cr: CriteriaQuery<SensorType> = cb.createQuery(SensorType::class.java)
            val root: Root<SensorType> = cr.from(SensorType::class.java)

            val deviceJoin = root.join<SensorType, Device>("device", JoinType.LEFT)

            val sensorTypePredicate = cb.equal(root.get<String>("uid"), sensorTypeUid)
            val devicePredicate = cb.equal(deviceJoin.get<String>("uuid"), deviceUuid)

            val finalPredicate: Predicate = cb.and(sensorTypePredicate, devicePredicate)

            cr.select(root).where(finalPredicate).distinct(true)

            val query = sess.createQuery(cr)
            return query.uniqueResultOptional().orElse(null)
        }
    }

}