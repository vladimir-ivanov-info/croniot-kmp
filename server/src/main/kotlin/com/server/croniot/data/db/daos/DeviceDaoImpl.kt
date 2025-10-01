package com.server.croniot.data.db.daos

import croniot.models.*
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.CriteriaQuery
import org.hibernate.SessionFactory
import javax.inject.Inject

class DeviceDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : DeviceDao {

    /*override fun insert(device: Device): Long {
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
    }*/

   /* override fun insert(account: Account, device: Device)/*: Long*/ {
        sessionFactory.openSession().use { session ->
            val tx = session.beginTransaction()
            try {
                // 1) Asegura que el Account está gestionado (si existe)
                /*device.account?.let { acc ->
                    if (acc.id != 0L) {
                        //val managedAcc = session.get(Account::class.java, acc.id)
                        //    ?: throw IllegalArgumentException("Account ${acc.id} no existe")
                        device.account = account
                    } else {
                        // Si el Account es nuevo y NO hay cascade desde Account->Device, persístelo primero o lanza error:
                        // session.persist(acc)
                        // (o) throw IllegalArgumentException("Account debe existir o persistirse antes")
                    }
                }*/

                device.account = account

                // 2) Sincroniza el lado hijo (bidireccional)
                device.sensorTypes.forEach { it.device = device }  // Hibernate asignará sensor_order (inverse=false)
                device.taskTypes.forEach { it.device = device }    // se insertarán por cascade si corresponde

                // 3) Persistir el padre (cascade insertará los hijos)
                session.persist(device)

                tx.commit()
                //return device.id
            } catch (e: Exception) {
                tx.rollback()
                throw e
            }
        }
    }*/



    override fun insert(account: Account, device: Device) {
        sessionFactory.openSession().use { session ->
            val tx = session.beginTransaction()
            try {
                // 1) Adjunta el Account
                val managedAcc = if (account.id != 0L) {
                    session.get(Account::class.java, account.id)
                        ?: throw IllegalArgumentException("Account ${account.id} no existe")
                } else {
                    account // nuevo
                }

                // 2) Relación bidireccional
                device.account = managedAcc
                managedAcc.devices.add(device)   // 👈 AQUÍ se fija el device_order

                // 3) Persistencia
                if (managedAcc === account && account.id == 0L) {
                    session.persist(managedAcc)  // cascade inserta device
                } else {
                    // Account ya existe: no hace falta persistirlo
                    // si tu cascade no cubre, puedes hacer session.persist(device)
                }

                tx.commit()
            } catch (e: Exception) {
                tx.rollback()
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
                sensorTypes = mutableListOf(), // Uninitialized
                taskTypes = mutableListOf(), // Uninitialized
                account = Account(), // Uninitialized
                deviceToken = null, // Uninitialized
            )
        }
    }
}
