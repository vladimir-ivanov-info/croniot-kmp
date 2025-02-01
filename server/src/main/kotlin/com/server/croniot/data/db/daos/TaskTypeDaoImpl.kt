package com.server.croniot.data.db.daos

import croniot.models.Device
import croniot.models.TaskType
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.*
import org.hibernate.SessionFactory
import javax.inject.Inject

class TaskTypeDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory
) : TaskTypeDao {

    override fun insert(task: TaskType): Long {
        val session = sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val taskId: Long
        try {
            session.persist(task)
            session.flush()
            taskId = task.id

            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }
        return taskId
    }

    override fun get(device: Device, taskTypeUid: Long) : TaskType? {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr : CriteriaQuery<TaskType> = cb.createQuery(TaskType::class.java)
            val root: Root<TaskType> = cr.from(TaskType::class.java)

            val taskUidPredicate = cb.equal(root.get<TaskType>("uid"), taskTypeUid)
            val deviceIdPredicate = cb.equal(root.get<TaskType>("device"), device)

            val finalPredicate: Predicate = cb.and(taskUidPredicate, deviceIdPredicate)
            cr.select(root).where(finalPredicate).distinct(true) //TODO maybe remove distinct

            val query = sess.createQuery(cr)
            //TODO try later query.setCacheable(true)
            val startMillis = System.currentTimeMillis()

            val result = query.uniqueResultOptional() //1100 ms

            val endMillis = System.currentTimeMillis()
            val time = endMillis - startMillis
            println("$time")

            return result.orElse(null)
        }
    }

    override fun getLazy(device: Device, taskTypeUid: Long): TaskType? {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr: CriteriaQuery<Tuple> = cb.createQuery(Tuple::class.java)

            val root = cr.from(TaskType::class.java)

            val taskUidPredicate = cb.equal(root.get<TaskType>("uid"), taskTypeUid)
            val deviceIdPredicate = cb.equal(root.get<TaskType>("device"), device)
            val finalPredicate: Predicate = cb.and(taskUidPredicate, deviceIdPredicate)

            cr.multiselect(root.get<Long>("id"), root.get<String>("uid")).where(finalPredicate)

            val query = sess.createQuery(cr)

            val tupleResult = query.uniqueResult() ?: return null

            return TaskType(
                id = tupleResult.get(0, Long::class.java),
                uid = tupleResult.get(1, Long::class.java),
                name = "",                  // Uninitialized
                description = "",           // Uninitialized
                parameters = mutableSetOf(),                // Uninitialized
                tasks = mutableSetOf(), // Uninitialized
                realTime = false,   // Uninitialized
                device = Device(),        // Uninitialized
            )
        }
    }

    override fun exists(device: Device, taskTypeUid: Long): Boolean {
        val session = sessionFactory.openSession()
        val startMillis = System.currentTimeMillis()

        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr: CriteriaQuery<Long> = cb.createQuery(Long::class.java)
            val root: Root<TaskType> = cr.from(TaskType::class.java)

            val taskUidPredicate = cb.equal(root.get<TaskType>("uid"), taskTypeUid)
            val deviceIdPredicate = cb.equal(root.get<TaskType>("device"), device)
            val finalPredicate: Predicate = cb.and(taskUidPredicate, deviceIdPredicate)

            cr.select(cb.count(root)).where(finalPredicate)

            val query = sess.createQuery(cr)
            val count = query.singleResult //2-7 ms

            val endMillis = System.currentTimeMillis()
            val time = endMillis - startMillis
            println("$time")
            return count > 0
        }
    }
}