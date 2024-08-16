package com.croniot.server.db.daos

import croniot.models.TaskStateInfo
import com.croniot.server.db.controllers.ControllerDb

class TaskStateInfoDaoImpl : TaskStateInfoDao {

    override fun insert(taskStateInfo: TaskStateInfo): Long {
        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val taskId: Long
        try {
            session.persist(taskStateInfo)
            session.flush()
            taskId = taskStateInfo.id

            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }
        return taskId
    }
}