package com.server.croniot.data.db.daos

import croniot.models.TaskStateInfo
import org.hibernate.SessionFactory
import javax.inject.Inject

class TaskStateInfoDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory
) : TaskStateInfoDao {

    override fun insert(taskStateInfo: TaskStateInfo): Long {
        val session = sessionFactory.openSession()
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