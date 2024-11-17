package db.daos

import com.croniot.server.db.controllers.ControllerDb
import croniot.models.*
import jakarta.persistence.Tuple
import jakarta.persistence.criteria.*
import org.hibernate.Session
import org.hibernate.Transaction
import kotlin.random.Random


class TaskDaoImpl: TaskDao {

    override fun create(device: Device, taskType: TaskType): Task {

        val taskUid = Random.nextLong(1, 10001)
        val task = Task(
            uid = taskUid,
            parametersValues = mutableMapOf(),
            taskType = taskType,
            stateInfos = mutableSetOf()
        )

        val session = ControllerDb.sessionFactory.openSession()
        val transaction = session.beginTransaction()
        try {
            session.persist(task)
            session.flush()
            transaction.commit()
        } catch (e: Exception) {
            transaction.rollback()
            throw e
        } finally {
            //session.close()
        }

        return task
    }

    override fun insert(task: Task): Long {
        var session: Session? = null
        var transaction: Transaction? = null
        var taskConfigurationId: Long

        try {
            session =
                ControllerDb.sessionFactory.openSession() // Open a new session for this thread
            transaction = session.beginTransaction() // Start a transaction

            session.persist(task) // Persist the task
            session.flush() // Ensure the task is inserted into the database

            taskConfigurationId = task.id // Get the generated ID

            transaction.commit() // Commit the transaction
        } catch (e: java.lang.Exception) {
            println(task.taskType)
            if (transaction != null) {
                transaction.rollback() // Rollback on exception
            }
            throw e // Rethrow the exception
        } finally {
            if (session != null) {
                session.close() // Always close the session
            }
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

            val query = sess.createQuery(cr) //4 ms
            return query.uniqueResult()
        }
    }

    override fun getLazy(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb: CriteriaBuilder = sess.criteriaBuilder
            val cr: CriteriaQuery<Tuple> = cb.createQuery(Tuple::class.java)
            val root: Root<Task> = cr.from(Task::class.java)

            // Define predicates
            val taskTypeJoin = root.join<Task, TaskType>("taskType", JoinType.LEFT)
            val devicePredicate = cb.equal(taskTypeJoin.get<Device>("device").get<String>("uuid"), deviceUuid)
            val taskTypePredicate = cb.equal(taskTypeJoin.get<Long>("uid"), taskTypeUid)
            val taskUidPredicate = cb.equal(root.get<Long>("uid"), taskUid)
            val finalPredicate: Predicate = cb.and(devicePredicate, taskTypePredicate, taskUidPredicate)

            // Select only `id` and `uid` fields
            cr.multiselect(root.get<Long>("id"), root.get<Long>("uid")).where(finalPredicate)

            val query = sess.createQuery(cr)
            //TODO bug: query.resultList gives 3 id values for uid = 2891
            val tupleResult = query.uniqueResult() ?: return null

            // Map results to a Task instance with only `id` and `uid` initialized
            return Task(
                id = tupleResult.get(0, Long::class.java),
                uid = tupleResult.get(1, Long::class.java),
                parametersValues = mutableMapOf(), // Uninitialized
                taskType = TaskType(),                   // Uninitialized
                stateInfos = mutableSetOf()        // Uninitialized
            )
        }
    }

    override fun getTaskWithIdOnly(taskUid: Long): Task? {
        val session = ControllerDb.sessionFactory.openSession()
        session.use { sess ->
            val cb: CriteriaBuilder = sess.criteriaBuilder
            val cr: CriteriaQuery<Long> = cb.createQuery(Long::class.java) // Select only `id`
            val root: Root<Task> = cr.from(Task::class.java)

            // Predicate for matching task UID
            val taskUidPredicate = cb.equal(root.get<Long>("uid"), taskUid)

            // Select only the `id` field
            cr.select(root.get("id")).where(taskUidPredicate)

            // Execute the query
            val query = sess.createQuery(cr)
            val idResult = query.uniqueResult() ?: return null

            // Return a new Task instance with only `id` populated
            return Task(id = idResult, uid = taskUid, parametersValues = mutableMapOf(), taskType = TaskType(), stateInfos = mutableSetOf())
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