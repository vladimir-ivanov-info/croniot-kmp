package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.DeviceToken
import org.hibernate.SessionFactory
import javax.inject.Inject

class DeviceTokenDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory
) : DeviceTokenDao {

    override fun insert(deviceToken: DeviceToken) {
        sessionFactory.openSession().use { session ->
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
        val session = sessionFactory.openSession()
        session.use { // This ensures the session is closed after use
            val cb = session.criteriaBuilder

            val cr = cb.createQuery(DeviceToken::class.java)
            val root = cr.from(DeviceToken::class.java)

            val tokenPredicate = cb.equal(root.get<String>("token"), token)
            cr.select(root).where(tokenPredicate)

            val query = session.createQuery(cr).uniqueResultOptional().orElse(null)
            return query?.device
        }
    }

}