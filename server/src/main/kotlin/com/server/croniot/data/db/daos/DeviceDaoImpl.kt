package com.server.croniot.data.db.daos

import croniot.models.*
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaQuery
import org.hibernate.SessionFactory
import javax.inject.Inject

class DeviceDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : DeviceDao {

    override fun insert(device: Device): Long {
        sessionFactory.openSession().use { session ->
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
        val session = sessionFactory.openSession()
        val cb = session.criteriaBuilder

        val cr = cb.createQuery(Device::class.java)
        val root = cr.from(Device::class.java)
        cr.select(root)

        val query = session.createQuery(cr)
        val list = query.resultList
        return list
    }

// TODO Method threw 'org.hibernate.NonUniqueResultException' exception.
    override fun getByUuid(deviceUuid: String): Device? {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Device::class.java)
            val root = cr.from(Device::class.java)

            val emailPredicate = cb.equal(root.get<String>("uuid"), deviceUuid)

            cr.select(root).where(emailPredicate)

            val query = sess.createQuery(cr).uniqueResultOptional() // TODO don't let register to the same device twice:  org.hibernate.NonUniqueResultException: Query did not return a unique result: 2 results were returned
            return query.orElse(null)
        }
    }

    override fun getLazy(deviceUuid: String): Device? {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr: CriteriaQuery<Tuple> = cb.createQuery(Tuple::class.java)

            val root = cr.from(Device::class.java)

            val uuidPredicate = cb.equal(root.get<String>("uuid"), deviceUuid)

            cr.multiselect(root.get<Long>("id"), root.get<String>("uuid")).where(uuidPredicate)

            val query = sess.createQuery(cr)
            val tupleResult = query.uniqueResult() ?: return null

            return Device(
                id = tupleResult.get(0, Long::class.java),
                uuid = tupleResult.get(1, String::class.java),
                name = "", // Uninitialized
                description = "", // Uninitialized
                iot = false, // Uninitialized
                sensorTypes = mutableSetOf(), // Uninitialized
                taskTypes = mutableSetOf(), // Uninitialized
                account = Account(), // Uninitialized
                deviceToken = null, // Uninitialized
            )
        }
    }
}
