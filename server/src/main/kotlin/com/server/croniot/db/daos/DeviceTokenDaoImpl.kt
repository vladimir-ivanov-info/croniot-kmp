package com.croniot.server.db.daos

import croniot.models.Device
import croniot.models.DeviceToken
import com.croniot.server.db.controllers.ControllerDb

class DeviceTokenDaoImpl : DeviceTokenDao {

    /*override fun insert(deviceToken: DeviceToken) {

        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()

        try {
            session.save(deviceToken)
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e // Rethrow the exception after rollback
        } finally {
            //session.close() // Close the session to release resources
        }
    }*/

    override fun insert(deviceToken: DeviceToken) {
        ControllerDb.sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            try {
                session.save(deviceToken)
                transaction.commit()
            } catch (e: Exception) {
                transaction.rollback()
                throw e
            }
        } // session is automatically closed here
    }


    override fun getDeviceAssociatedWithToken(token: String): Device? {

        val session = ControllerDb.sessionFactory.openSession()
        session.use { // This ensures the session is closed after use
            val cb = session.criteriaBuilder

            val cr = cb.createQuery(DeviceToken::class.java)
            val root = cr.from(DeviceToken::class.java)

            val tokenPredicate = cb.equal(root.get<String>("token"), token)
            cr.select(root).where(tokenPredicate)

            /*val query = session.createQuery(cq)
            val result = query.resultList

            if (result.isNotEmpty()) {
                return result.first().device
            }*/

            val query = session.createQuery(cr).uniqueResultOptional().orElse(null)
            //return query.orElse(null).device
            return query?.device
        }
        //return null
    }

}