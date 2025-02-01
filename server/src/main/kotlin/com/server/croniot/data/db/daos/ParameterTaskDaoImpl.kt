package com.server.croniot.data.db.daos

import croniot.models.ParameterTask
import croniot.models.TaskType
import jakarta.persistence.Tuple
import org.hibernate.SessionFactory
import javax.inject.Inject

class ParameterTaskDaoImpl @Inject constructor(
    private val sessionFactory: SessionFactory,
) : ParameterTaskDao {

    override fun getByUid(parameterTaskUid: Long, taskType: TaskType): ParameterTask? {
        val session = sessionFactory.openSession()

        session.use { sess ->
            val cb = sess.criteriaBuilder
            val cr = cb.createQuery(Tuple::class.java)
            val root = cr.from(ParameterTask::class.java)

            val taskUidPredicate = cb.equal(root.get<ParameterTask>("uid"), parameterTaskUid)
            val taskTypeIdPredicate = cb.equal(root.get<ParameterTask>("taskTypeId"), taskType.id)

            cr.multiselect(
                root.get<Long>("id"),
                root.get<Long>("uid"),
                root.get<String>("name"),
                root.get<String>("type"),
                root.get<String>("unit"),
                root.get<String>("description"),
                // root.get<Map<String, String>>("constraints")
            ).where(cb.and(taskUidPredicate, taskTypeIdPredicate))

            val query = sess.createQuery(cr)
            val tupleResult = query.uniqueResult() ?: return null

            val parameterTask = ParameterTask(
                id = tupleResult.get(0, Long::class.java),
                uid = tupleResult.get(1, Long::class.java),
                name = tupleResult.get(2, String::class.java),
                type = tupleResult.get(3, String::class.java),
                unit = tupleResult.get(4, String::class.java),
                description = tupleResult.get(5, String::class.java),
                // constraints = tupleResult.get(6) as MutableMap<String, String>,
                taskTypeId = taskType.id, // Set the taskTypeId manually
                // Do not set taskType here
            )

            if (parameterTask != null) {
                parameterTask.constraints = getConstraints(parameterTask.id)
            }

            return parameterTask
        }
    }

    private fun getConstraints(parameterTaskId: Long): MutableMap<String, String> {
        val session = sessionFactory.openSession()
        session.use { sess ->
            val sql = """
            SELECT constraint_key, constraint_value
            FROM parameter_task_constraints
            WHERE parameter_id = :parameterTaskId
            """.trimIndent()

            val query = sess.createNativeQuery(sql)
            query.setParameter("parameterTaskId", parameterTaskId)

            val results = query.list() as List<Array<Any>>

            return results.associate { row ->
                val key = row[0] as String
                val value = row[1] as String
                key to value
            }.toMutableMap()
        }
    }

/* override fun getLazy(parameterTaskUid: Long, taskType: TaskType): ParameterTask? {

 val session = ControllerDb.sessionFactory.openSession()
 session.use { sess ->
     val cb = sess.criteriaBuilder
     // val cr = cb.createQuery(Array<Any>::class.java)
     val cr: CriteriaQuery<Tuple> = cb.createQuery(Tuple::class.java)

     val root = cr.from(ParameterTask::class.java)

   //  val constraintsJoin = root.join<Map.Entry<String, String>>("constraints", JoinType.LEFT)
  //   val constraintsJoin = root.join<Map.Entry<String, String>>("constraints", JoinType.LEFT)


     // Define predicate for matching the `uuid`
     val taskUidPredicate = cb.equal(root.get<ParameterTask>("uid"), parameterTaskUid)
     val taskTypePredicate = cb.equal(root.get<ParameterTask>("taskType"), taskType)

     //val deviceIdPredicate = cb.equal(root.get<ParameterTask>("device"), device)
 //    val taskTypeJoin = root.join<Task, TaskType>("taskType", JoinType.LEFT)
  //   val devicePredicate = cb.equal(taskTypeJoin.get<Device>("device").get<String>("uuid"), deviceUuid)

     val finalPredicate: Predicate = cb.and(taskUidPredicate, taskTypePredicate)
     // Select only `id` and `uid` fields
     cr.multiselect(
         root.get<Long>("id"),
         root.get<String>("uid"),
         root.get<String>("name"),
         root.get<String>("unit"),
         root.get<String>("description"),
         root.get<MutableMap<String, String>>("constraints"),
     ).where(finalPredicate)

     val query = sess.createQuery(cr)
     //val result = query.uniqueResult() ?: return null
     val tupleResult = query.uniqueResult() ?: return null
     // Map the projection result to a `Device` instance with only `id` and `uuid` initialized
     return ParameterTask(
         id = tupleResult.get(0, Long::class.java),
         uid = tupleResult.get(1, Long::class.java),
         name = tupleResult.get(2, String::class.java),                  // Uninitialized
         type = tupleResult.get(3, String::class.java),           // Uninitialized
         unit = tupleResult.get(4, String::class.java),                // Uninitialized
         description = tupleResult.get(5, String::class.java), // Uninitialized
         constraints = tupleResult.get(6, MutableMap::class.java),   // Uninitialized
         taskType = TaskType(),        // Uninitialized
     )
 }

}*/
/*
fun getByUid2(parameterTaskUid: Long, taskType: TaskType) : ParameterTask? {
 val session = ControllerDb.sessionFactory.openSession()
 session.use { sess ->
     val cb = sess.criteriaBuilder

     // First query: Retrieve the `ParameterTask` entity without fetching `constraints`
     val parameterTaskCriteria = cb.createQuery(ParameterTask::class.java)
     val parameterTaskRoot = parameterTaskCriteria.from(ParameterTask::class.java)

     val taskUidPredicate = cb.equal(parameterTaskRoot.get<Long>("uid"), parameterTaskUid)
     val taskTypeIdPredicate = cb.equal(parameterTaskRoot.get<TaskType>("taskType").get<Long>("id"), taskType.id)

     parameterTaskCriteria.select(parameterTaskRoot).where(cb.and(taskUidPredicate, taskTypeIdPredicate))

     val parameterTask = sess.createQuery(parameterTaskCriteria).uniqueResult() ?: return null

     // Second query: Retrieve `constraints` as a list of key-value pairs
     val constraintsCriteria = cb.createQuery(Tuple::class.java)
     val constraintsRoot = constraintsCriteria.from(ParameterTask::class.java)
   /*  val constraintsJoin = constraintsRoot.join<Map.Entry<String, String>>("constraints")

     constraintsCriteria.multiselect(
         constraintsJoin.key(),
         constraintsJoin.value()
     ).where(cb.equal(constraintsRoot.get<Long>("id"), parameterTask.id))*/

     val constraintsQuery = sess.createQuery(constraintsCriteria)
     val constraintsList = constraintsQuery.resultList

     // Convert the list of tuples into a map
     val constraintsMap = constraintsList.associate { tuple ->
         tuple.get(0, String::class.java) to tuple.get(1, String::class.java)
     }.toMutableMap()

     // Assign the populated constraints map to the `ParameterTask` instance
     parameterTask.constraints = constraintsMap

     return parameterTask
 }
}*/
}
