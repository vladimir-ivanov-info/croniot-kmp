package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.ParameterTask.Companion.PARAMETER_TASK
import com.server.croniot.jooq.tables.Task.Companion.TASK
import com.server.croniot.jooq.tables.TaskParameterValue.Companion.TASK_PARAMETER_VALUE
import com.server.croniot.jooq.tables.TaskStateInfo.Companion.TASK_STATE_INFO
import com.server.croniot.jooq.tables.TaskType.Companion.TASK_TYPE
import croniot.models.ParameterTask
import croniot.models.Task
import croniot.models.TaskStateInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import java.time.ZoneId
import javax.inject.Inject
import kotlin.random.Random

class TaskDaoJooqImpl @Inject constructor(
    private val dsl: DSLContext,
) : TaskDao {

    override fun create(taskTypeId: Long, taskTypeUid: Long): Task? {
        val taskUid = Random.nextLong(from = 0, until = Long.MAX_VALUE)

        val insertedId: Long? = dsl.transactionResult { config ->
            val tx = using(config)

            tx.insertInto(TASK)
                .set(TASK.UID, taskUid)
                .set(TASK.TASK_TYPE, taskTypeId)
                .returning(TASK.ID)
                .fetchOne()
                ?.get(TASK.ID)
        }

        if (insertedId == null) {
            return null
        }

        return Task(
            uid = taskUid,
            taskTypeUid = taskTypeUid,
            parametersValues = mutableMapOf(),
        )
    }

    override fun insert(task: Task): Long {
        // TODO implement jOOQ insert
        return -1
    }

    override fun get(deviceUuid: String, taskTypeUid: Long, taskUid: Long): Task? {
        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val base = tx
                .select(
                    TASK.ID,
                    TASK.UID,
                    TASK.TASK_TYPE,
                    TASK_TYPE.ID,
                    TASK_TYPE.UID,
                    TASK_TYPE.NAME,
                    TASK_TYPE.DESCRIPTION,
                    TASK_TYPE.DEVICE,
                )
                .from(TASK)
                .join(TASK_TYPE).on(TASK.TASK_TYPE.eq(TASK_TYPE.ID))
                .join(DEVICE).on(TASK_TYPE.DEVICE.eq(DEVICE.ID))
                .where(DEVICE.UUID.eq(deviceUuid))
                .and(TASK_TYPE.UID.eq(taskTypeUid))
                .and(TASK.UID.eq(taskUid))
                .fetchOne()
                ?: return@transactionResult null

            val taskId: Long = base.get(TASK.ID)!!
            val taskTypeId: Long = base.get(TASK_TYPE.ID)!!

            val stateInfoRecs = tx
                .selectFrom(TASK_STATE_INFO)
                .where(TASK_STATE_INFO.TASK.eq(taskId))
                .orderBy(TASK_STATE_INFO.DATE_TIME.asc())
                .fetch()

            val paramTaskRecs = tx
                .selectFrom(PARAMETER_TASK)
                .where(PARAMETER_TASK.TASK_TYPE.eq(taskTypeId))
                .fetch()

            val paramValueRecs = tx
                .selectFrom(TASK_PARAMETER_VALUE)
                .where(TASK_PARAMETER_VALUE.ID_TASK.eq(taskId))
                .fetch()

            val paramById: MutableMap<Long, ParameterTask> = mutableMapOf()

            for (pRec in paramTaskRecs) {
                val pId = pRec.id ?: continue
                val constraints: MutableMap<String, String> = mutableMapOf()

                val p = ParameterTask(
                    uid = pRec.uid ?: 0L,
                    name = pRec.name ?: "",
                    type = pRec.type ?: "",
                    unit = pRec.unit ?: "",
                    description = pRec.description ?: "",
                    constraints = constraints,
                )
                paramById[pId] = p
            }

            val taskTypeUidFromDb = base.get(TASK_TYPE.UID) ?: 0L

            val parametersValues: MutableMap<ParameterTask, String> = mutableMapOf()

            for (vRec in paramValueRecs) {
                val pId = vRec.idParameter ?: continue
                val value = vRec.value ?: continue
                val param = paramById[pId] ?: continue
                parametersValues[param] = value
            }

            val taskUidFromDb = base.get(TASK.UID)!!
            val task = Task(
                uid = taskUidFromDb,
                parametersValues = parametersValues,
                taskTypeUid = taskTypeUidFromDb,
            )

            for (sRec in stateInfoRecs) {
                TaskStateInfo(
                    taskUid = taskUidFromDb,
                    dateTime = sRec.dateTime!!
                        .atZoneSameInstant(ZoneId.systemDefault()),
                    state = sRec.state ?: "",
                    progress = sRec.progress ?: 0.0,
                    errorMessage = sRec.errorMessage ?: "",
                )
            }

            task
        }
    }

    override fun getAll(deviceUuid: String): List<Task> {
        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val taskRows = tx
                .select(
                    TASK.ID,
                    TASK.UID,
                    TASK_TYPE.UID
                )
                .from(TASK)
                .join(TASK_TYPE).on(TASK.TASK_TYPE.eq(TASK_TYPE.ID))
                .join(DEVICE).on(TASK_TYPE.DEVICE.eq(DEVICE.ID))
                .where(DEVICE.UUID.eq(deviceUuid))
                .fetch()

            if (taskRows.isEmpty()) return@transactionResult emptyList()

            val taskIds = taskRows.mapNotNull { it.get(TASK.ID) }
            if (taskIds.isEmpty()) return@transactionResult emptyList()

            val taskUidByTaskId: Map<Long, Long> =
                taskRows
                    .mapNotNull { r ->
                        val id = r.get(TASK.ID)
                        val uid = r.get(TASK.UID)
                        if (id != null && uid != null) id to uid else null
                    }
                    .toMap()

            val tsiRecs = tx
                .selectFrom(TASK_STATE_INFO)
                .where(TASK_STATE_INFO.TASK.`in`(taskIds))
                .orderBy(TASK_STATE_INFO.DATE_TIME.asc())
                .fetch()

            val stateInfosByTaskId: Map<Long, MutableList<TaskStateInfo>> =
                tsiRecs
                    .filter { it.task != null && it.dateTime != null }
                    .groupBy { it.task!! }
                    .mapValues { (taskId, recs) ->
                        val taskUid = taskUidByTaskId[taskId] ?: 0L

                        recs.map { rec ->
                            val zdt = rec.dateTime!!.toZonedDateTime()

                            TaskStateInfo(
                                taskUid = taskUid,
                                dateTime = zdt,
                                state = rec.state ?: "",
                                progress = rec.progress ?: 0.0,
                                errorMessage = rec.errorMessage ?: ""
                            )
                        }.toMutableList()
                    }

            taskRows.map { r ->
                val taskId = r.get(TASK.ID)!!
                val taskUid = r.get(TASK.UID)!!
                val taskTypeUid = r.get(TASK_TYPE.UID)!!
                val mostRecentStateInfo = stateInfosByTaskId[taskId]?.maxByOrNull { it.dateTime }

                Task(
                    uid = taskUid,
                    parametersValues = mutableMapOf(),
                    taskTypeUid = taskTypeUid,
                    mostRecentStateInfo = mostRecentStateInfo,
                )
            }
        }
    }
}
