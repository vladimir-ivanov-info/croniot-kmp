package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.SensorType
import croniot.models.TaskType
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.hibernate.SessionFactory
import javax.inject.Inject

class SensorTypeDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : SensorTypeDao {

    /*override fun insert(device: Device, sensorType: SensorType) /*: Long*/ {
        val session = sessionFactory.openSession()
        val transaction = session.beginTransaction()
        //val sensorId: Long

        device.sensorTypes.add(sensorType)

        try {
            session.persist(device)
                //session.persist(sensorType)
            session.flush()
            //sensorId = sensorType.id

            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            // session.close()
        }
        //return sensorId
    }*/

   /* override fun insert(device: Device, sensorType: SensorType) {
        sessionFactory.openSession().use { session ->
            val tx = session.beginTransaction()
            try {
                // Adjunta un Device gestionado a la sesión
                val managedDevice = when {
                    device.id != 0L -> session.get(Device::class.java, device.id)
                    else -> device // si es nuevo
                } ?: throw IllegalArgumentException("Device ${device.id} no existe")

                // relación bidireccional
                sensorType.device = managedDevice
                managedDevice.sensorTypes.add(sensorType) // Hibernate pondrá sensor_order





            //    for(sensorType.parameters)





                // Persistir:
                if (managedDevice === device && device.id == 0L) {
                    // device nuevo: persistimos el padre; cascada insertará al hijo
                    session.persist(managedDevice)
                } else {
                    // device ya existente: NO hace falta persist; el add() marca dirty
                    // si tu cascada no es 'save-update'/'all', usa session.persist(sensorType)
                }

                tx.commit()
            } catch (e: Exception) {
                tx.rollback()
                throw e
            }
        }
    }*/

//    override fun insert(device: Device, sensorType: SensorType)/*: Long*/ {
//        sessionFactory.openSession().use { session ->
//            val tx = session.beginTransaction()
//            try {
//                // 1. Recuperar el Device gestionado
//                //val device = task.device
//                //    ?: throw IllegalArgumentException("TaskType debe tener un Device asignado")
//
//                /*val managedDevice = if (device.id != 0L) {
//                    session.get(Device::class.java, device.id)
//                        ?: throw IllegalArgumentException("Device ${device.id} no existe")
//                } else {
//                    device
//                }*/
//
//                // 2. Relación bidireccional
//                sensorType.device = device
//                device.sensorTypes.add(sensorType)
//
//                // 3. Persistencia
//                /*if (device.id == 0L) {
//                    // Device nuevo, persistimos el padre con cascade
//                    session.persist(device)
//                } else {
//                    // Device existente, no hace falta persistir; cascade se encargará del hijo
//                }*/
//                session.persist(device)
//                tx.commit()
//                //return task.id
//            } catch (e: Exception) {
//                tx.rollback()
//                throw e
//            }
//        }
//    }



    /*override fun insert(device: Device, sensorType: SensorType) {
        sessionFactory.openSession().use { session ->
            val tx = session.beginTransaction()
            try {
                // 1) Asegúrate de trabajar con un Device gestionado
                val managedDevice = session.get(Device::class.java, device.id)
                    ?: throw IllegalArgumentException("Device ${device.id} no existe")

                // 2) Sincroniza ambos lados
                sensorType.device = managedDevice
                managedDevice.sensorTypes.add(sensorType)  // <- inverse=false → Hibernate asigna sensor_order

                // 3) Persistir explícitamente el hijo SOLO si no tienes cascade save-update/all
                // session.persist(sensorType)  // Descomenta si tu mapping NO tiene cascade suficiente

                tx.commit() // flush implícito
            } catch (e: Exception) {
                tx.rollback(); throw e
            }
        }
    }*/

    /*override fun insert(device: Device, sensorType: SensorType) {
        sessionFactory.openSession().use { session ->
            val tx = session.beginTransaction()
            try {
                // 1) Device gestionado
                val managedDevice = session.get(Device::class.java, device.id)
                    ?: throw IllegalArgumentException("Device ${device.id} no existe")

                // 2) Back-ref + “tocar” la lista para que Hibernate vea inserciones
                sensorType.device = managedDevice

                // MUY IMPORTANTE: asegurar backref y que la colección se modifica en esta sesión
                val params = ArrayList(sensorType.parameters)   // copia el contenido “pre-creado”
                sensorType.parameters.clear()                   // vacía (marca diff)
                params.forEach { p ->
                    p.sensorType = sensorType                   // back-reference
                    sensorType.parameters.add(p)                // add() dentro de sesión → asigna param_order 0..N-1
                }

                // 3) Añadir el SensorType al padre (list inverse=false → pone sensor_order)
                managedDevice.sensorTypes.add(sensorType)

                // 4) Con cascade en Device.sensorTypes y SensorType.parameters no necesitas persist explícito
                tx.commit()                                     // flush implícito: INSERT de sensor_type + parameter_sensor con índices
            } catch (e: Exception) {
                tx.rollback(); throw e
            }
        }
    }*/

    override fun insert(device: Device, sensorType: SensorType) {
        sessionFactory.openSession().use { s ->
            val tx = s.beginTransaction()
            try {
                val managedDevice = s.get(Device::class.java, device.id)
                    ?: error("Device ${device.id} no existe")

                // Asegura backref antes de añadir al padre
                sensorType.parameters.forEach { it.sensorType = sensorType }

                // Añade el SensorType al padre (inverse=false → Hibernate pondrá sensor_type_order)
                sensorType.device = managedDevice
                managedDevice.sensorTypes.add(sensorType)

                // No hace falta persist explícito si hay cascade=all/all-delete-orphan
                tx.commit()
            } catch (e: Exception) {
                tx.rollback(); throw e
            }
        }
    }






    override fun getLazy(deviceUuid: String, sensorTypeUid: Long): SensorType? {
        val session = sessionFactory.openSession()
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
