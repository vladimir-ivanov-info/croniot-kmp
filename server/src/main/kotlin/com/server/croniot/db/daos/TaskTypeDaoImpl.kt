package com.croniot.server.db.daos

import croniot.models.Device
import croniot.models.TaskType
import com.croniot.server.db.controllers.ControllerDb

class TaskTypeDaoImpl : TaskTypeDao {

    override fun insert(task: TaskType): Long {
        val session = ControllerDb.sessionFactory.openSession()
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

    override fun get(device: Device, taskUid: Long) : TaskType? {

        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(TaskType::class.java)
            val root = cr.from(TaskType::class.java)

            val taskUidPredicate = cb.equal(root.get<TaskType>("uid"), taskUid)
            val deviceIdPredicate = cb.equal(root.get<TaskType>("device"), device)

            cr.select(root).where(cb.and(taskUidPredicate, deviceIdPredicate))

            val result = sess.createQuery(cr).resultList

            return if (result.isNotEmpty()) result.first() else null
        }
    }

}