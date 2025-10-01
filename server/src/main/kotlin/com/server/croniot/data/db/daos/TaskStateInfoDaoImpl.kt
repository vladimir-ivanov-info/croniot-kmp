package com.server.croniot.data.db.daos

import croniot.models.Task
import croniot.models.TaskStateInfo
import org.hibernate.SessionFactory
import javax.inject.Inject

class TaskStateInfoDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : TaskStateInfoDao {

    /*override fun insert(taskStateInfo: TaskStateInfo): Long {
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
            // session.close()
        }
        return taskId
    }*/

    override fun insert(task: Task, taskStateInfo: TaskStateInfo): Long {
        sessionFactory.openSession().use { session ->
            val tx = session.beginTransaction()
            try {
                // 1) El padre debe venir seteado
                // val task = taskStateInfo.task
                //    ?: throw IllegalArgumentException("TaskStateInfo debe tener 'task' asignado")

                // 2) Adjunta el Task gestionado si ya existe
                val managedTask = if (task.id != 0L) {
                    session.get(Task::class.java, task.id)
                        ?: throw IllegalArgumentException("Task ${task.id} no existe")
                } else {
                    task // es nuevo
                }

                // 3) Sincroniza relación bidireccional y añade a la lista del padre
                taskStateInfo.task = managedTask
                managedTask.stateInfos.add(taskStateInfo) // Hibernate pondrá state_order

                // 4) Persistencia
                if (managedTask === task && task.id == 0L) {
                    session.persist(managedTask) // cascade inserta el hijo
                } // si el Task ya existía, no hace falta persistir explícitamente

                tx.commit()
                return taskStateInfo.id
            } catch (e: Exception) {
                tx.rollback()
                throw e
            }
        }
    }
}
