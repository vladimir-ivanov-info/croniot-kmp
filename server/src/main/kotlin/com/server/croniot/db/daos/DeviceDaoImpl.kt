package com.croniot.server.db.daos

import croniot.models.*
import com.croniot.server.db.controllers.ControllerDb
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root


class DeviceDaoImpl: DeviceDao {

    /*override fun insert(device: Device) : Long {
        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val deviceId: Long
        try {
            session.persist(device)
            session.flush()
            deviceId = device.id
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }
        return deviceId
    }*/

    override fun insert(device: Device): Long {
        ControllerDb.sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            return try {
                session.persist(device)
                transaction.commit()
                device.id // Return device ID directly
            } catch (e: Exception) {
                transaction.rollback()
                throw e
            }
        }
    }


    override fun getAll(): List<Device> {
        val session = ControllerDb.sessionFactory.openSession()
        val cb = session.criteriaBuilder

        val cr = cb.createQuery(Device::class.java)
        val root = cr.from(Device::class.java)
        cr.select(root)

        val query = session.createQuery(cr)
        val list = query.resultList
        return list
    }
//TODO Method threw 'org.hibernate.NonUniqueResultException' exception.
    override fun getByUuid(deviceUuid: String) : Device? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Device::class.java)
            val root = cr.from(Device::class.java)

            val emailPredicate = cb.equal(root.get<String>("uuid"), deviceUuid)

            cr.select(root).where(emailPredicate)

            //val result = sess.createQuery(cr).resultList

            //return if (result.isNotEmpty()) result.first() else null
            val query = sess.createQuery(cr).uniqueResultOptional()
            return query.orElse(null)
        }
    }

    override fun getLazy(deviceUuid: String): Device? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
           // val cr = cb.createQuery(Array<Any>::class.java)
            val cr: CriteriaQuery<Tuple> = cb.createQuery(Tuple::class.java)

            val root = cr.from(Device::class.java)

            // Define predicate for matching the `uuid`
            val uuidPredicate = cb.equal(root.get<String>("uuid"), deviceUuid)

            // Select only `id` and `uuid` fields
            cr.multiselect(root.get<Long>("id"), root.get<String>("uuid")).where(uuidPredicate)

            val query = sess.createQuery(cr)
            //val result = query.uniqueResult() ?: return null
            val tupleResult = query.uniqueResult() ?: return null
            // Map the projection result to a `Device` instance with only `id` and `uuid` initialized
            return Device(
                id = tupleResult.get(0, Long::class.java),
                uuid = tupleResult.get(1, String::class.java),
                name = "",                  // Uninitialized
                description = "",           // Uninitialized
                iot = false,                // Uninitialized
                sensorTypes = mutableSetOf(), // Uninitialized
                taskTypes = mutableSetOf(),   // Uninitialized
                account = Account(),        // Uninitialized
                deviceToken = null          // Uninitialized
            )
        }
    }
}