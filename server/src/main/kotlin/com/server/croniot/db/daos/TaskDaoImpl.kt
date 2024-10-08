package db.daos

import croniot.models.*
import com.croniot.server.db.controllers.ControllerDb
import jakarta.persistence.criteria.*

class TaskDaoImpl: TaskDao {

    override fun insert(task: Task): Long {
        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()
        val taskConfigurationId: Long
        try {
            session.persist(task)
            session.flush()
            taskConfigurationId = task.id

            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }
        return taskConfigurationId
    }

    override fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long) : Task? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb: CriteriaBuilder = sess.criteriaBuilder
            val cr: CriteriaQuery<Task> = cb.createQuery(Task::class.java)
            val root: Root<Task> = cr.from(Task::class.java)

            val taskJoin = root.join<Task, TaskType>("taskType", JoinType.LEFT)
            val deviceJoin = taskJoin.join<TaskType, Device>("device", JoinType.LEFT)

            val devicePredicate = cb.equal(deviceJoin.get<String>("uuid"), deviceUuid)
            val taskTypePredicate = cb.equal(taskJoin.get<Long>("uid"), taskTypeUid)
            val taskUidPredicate = cb.equal(root.get<Long>("uid"), taskUid)

            val finalPredicate: Predicate = cb.and(devicePredicate, taskTypePredicate, taskUidPredicate)

            cr.select(root).where(finalPredicate).distinct(true)

            val query = sess.createQuery(cr)
            return query.uniqueResult()
        }
    }

    override fun getAll(deviceUuid: String): List<Task> {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb: CriteriaBuilder = sess.criteriaBuilder
            val cr: CriteriaQuery<Task> = cb.createQuery(Task::class.java)
            val root: Root<Task> = cr.from(Task::class.java)

            val taskJoin = root.join<Task, TaskType>("taskType", JoinType.LEFT)
            val deviceJoin = taskJoin.join<TaskType, Device>("device", JoinType.LEFT)

            val devicePredicate = cb.equal(deviceJoin.get<String>("uuid"), deviceUuid)

            cr.select(root).where(devicePredicate).distinct(true)

            val query = sess.createQuery(cr)
            val taskConfigurations = query.resultList

            return taskConfigurations
        }
    }
}