package com.croniot.server.db.daos

import croniot.models.*
import com.croniot.server.db.controllers.ControllerDb

class DeviceDaoImpl: DeviceDao {

    override fun insert(device: Device) : Long {
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

    override fun getByUuid(deviceUuid: String) : Device? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Device::class.java)
            val root = cr.from(Device::class.java)

            val emailPredicate = cb.equal(root.get<String>("uuid"), deviceUuid)

            cr.select(root).where(emailPredicate)

            val result = sess.createQuery(cr).resultList

            return if (result.isNotEmpty()) result.first() else null
        }
    }
}