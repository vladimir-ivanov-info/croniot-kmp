package com.croniot.server.db.daos

import croniot.models.ParameterTask
import croniot.models.TaskType
import com.croniot.server.db.controllers.ControllerDb

class ParameterTaskDaoImpl : ParameterTaskDao {

    override fun getByUid(parameterTaskUid: Long, task: TaskType): ParameterTask? {

        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(ParameterTask::class.java)
            val root = cr.from(ParameterTask::class.java)

            val taskUidPredicate = cb.equal(root.get<ParameterTask>("uid"), parameterTaskUid)
            val deviceIdPredicate = cb.equal(root.get<ParameterTask>("taskType"), task)

            cr.select(root).where(cb.and(taskUidPredicate, deviceIdPredicate))

            val result = sess.createQuery(cr).resultList

            return if (result.isNotEmpty()) result.first() else null
        }

    }
}