package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.ParameterTask.Companion.PARAMETER_TASK
import com.server.croniot.jooq.tables.ParameterTaskConstraints.Companion.PARAMETER_TASK_CONSTRAINTS
import com.server.croniot.jooq.tables.TaskType.Companion.TASK_TYPE
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.TaskType
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import javax.inject.Inject

class TaskTypeDaoJooqImpl @Inject constructor(
    private val dsl: DSLContext,
) : TaskTypeDao {

    override fun upsert(taskType: TaskType, deviceId: Long): Long {
        require(deviceId != 0L) { "deviceId (PK interna) es obligatorio." }

        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val existingTaskTypeId: Long? = tx
                .select(TASK_TYPE.ID)
                .from(TASK_TYPE)
                .where(TASK_TYPE.UID.eq(taskType.uid))
                .and(TASK_TYPE.DEVICE.eq(deviceId))
                .fetchOne(TASK_TYPE.ID)

            val taskTypeId: Long =
                if (existingTaskTypeId != null) {
                    tx.update(TASK_TYPE)
                        .set(TASK_TYPE.NAME, taskType.name)
                        .set(TASK_TYPE.DESCRIPTION, taskType.description)
                        .where(TASK_TYPE.ID.eq(existingTaskTypeId))
                        .execute()

                    existingTaskTypeId
                } else {
                    val rec = tx.insertInto(TASK_TYPE)
                        .set(TASK_TYPE.UID, taskType.uid)
                        .set(TASK_TYPE.NAME, taskType.name)
                        .set(TASK_TYPE.DESCRIPTION, taskType.description)
                        .set(TASK_TYPE.DEVICE, deviceId)
                        .returning(TASK_TYPE.ID)
                        .fetchOne()
                        ?: error("INSERT TASK_TYPE no devolvio registro")

                    rec[TASK_TYPE.ID] ?: error("INSERT TASK_TYPE devolvio ID nulo")
                }

            for (p in taskType.parameters) {
                val existingParamId: Long? = tx
                    .select(PARAMETER_TASK.ID)
                    .from(PARAMETER_TASK)
                    .where(PARAMETER_TASK.UID.eq(p.uid))
                    .and(PARAMETER_TASK.TASK_TYPE.eq(taskTypeId))
                    .fetchOne(PARAMETER_TASK.ID)

                val parameterId: Long =
                    if (existingParamId != null) {
                        tx.update(PARAMETER_TASK)
                            .set(PARAMETER_TASK.NAME, p.name)
                            .set(PARAMETER_TASK.TYPE, p.type)
                            .set(PARAMETER_TASK.UNIT, p.unit)
                            .set(PARAMETER_TASK.DESCRIPTION, p.description)
                            .where(PARAMETER_TASK.ID.eq(existingParamId))
                            .execute()

                        existingParamId
                    } else {
                        val pRec = tx.insertInto(PARAMETER_TASK)
                            .set(PARAMETER_TASK.UID, p.uid)
                            .set(PARAMETER_TASK.NAME, p.name)
                            .set(PARAMETER_TASK.TYPE, p.type)
                            .set(PARAMETER_TASK.UNIT, p.unit)
                            .set(PARAMETER_TASK.DESCRIPTION, p.description)
                            .set(PARAMETER_TASK.TASK_TYPE, taskTypeId)
                            .returning(PARAMETER_TASK.ID)
                            .fetchOne()
                            ?: error("INSERT PARAMETER_TASK no devolvio registro")

                        pRec[PARAMETER_TASK.ID] ?: error("INSERT PARAMETER_TASK devolvio ID nulo")
                    }

                tx.deleteFrom(PARAMETER_TASK_CONSTRAINTS)
                    .where(PARAMETER_TASK_CONSTRAINTS.PARAMETER_ID.eq(parameterId))
                    .execute()

                val cons = p.constraints
                if (cons.isNotEmpty()) {
                    val inserts = cons
                        .filterKeys { it.isNotBlank() }
                        .map { (k, v) ->
                            tx.insertInto(PARAMETER_TASK_CONSTRAINTS)
                                .set(PARAMETER_TASK_CONSTRAINTS.PARAMETER_ID, parameterId)
                                .set(PARAMETER_TASK_CONSTRAINTS.CONSTRAINT_KEY, k)
                                .set(PARAMETER_TASK_CONSTRAINTS.CONSTRAINT_VALUE, v)
                        }

                    tx.batch(inserts).execute()
                }
            }

            taskTypeId
        }
    }

    override fun getByDeviceIds(
        deviceIds: List<Long>
    ): Map<Long, List<TaskType>> {
        if (deviceIds.isEmpty()) return emptyMap()

        return dsl.transactionResult { cfg ->
            val tx = org.jooq.impl.DSL.using(cfg)

            val ttRecs = tx
                .selectFrom(TASK_TYPE)
                .where(TASK_TYPE.DEVICE.`in`(deviceIds))
                .fetch()

            if (ttRecs.isEmpty()) return@transactionResult emptyMap()

            val taskTypeIds = ttRecs.mapNotNull { it.id }
            if (taskTypeIds.isEmpty()) return@transactionResult emptyMap()

            val pRecs = tx
                .selectFrom(PARAMETER_TASK)
                .where(PARAMETER_TASK.TASK_TYPE.`in`(taskTypeIds))
                .fetch()

            val paramIds = pRecs.mapNotNull { it.id }

            val cRecs =
                if (paramIds.isEmpty()) {
                    emptyList()
                } else {
                    tx.selectFrom(PARAMETER_TASK_CONSTRAINTS)
                        .where(PARAMETER_TASK_CONSTRAINTS.PARAMETER_ID.`in`(paramIds))
                        .fetch()
                }

            val paramsByTaskTypeId =
                pRecs
                    .filter { it.taskType != null && it.id != null }
                    .groupBy { it.taskType!! }

            val constraintsByParamId =
                cRecs
                    .filter { it.parameterId != null }
                    .groupBy { it.parameterId!! }

            val result: MutableMap<Long, MutableList<TaskType>> = mutableMapOf()

            for (ttRec in ttRecs) {
                val ttId = ttRec.id ?: continue
                val deviceId = ttRec.device ?: continue

                val domainParams = mutableListOf<ParameterTask>()

                val domainTaskType = TaskType(
                    uid = ttRec.uid ?: 0L,
                    name = ttRec.name ?: "",
                    description = ttRec.description ?: "",
                    parameters = domainParams
                )

                val paramRecsForTt =
                    paramsByTaskTypeId[ttId] ?: emptyList()

                for (pRec in paramRecsForTt) {
                    val pId = pRec.id ?: continue

                    val constraintRows =
                        constraintsByParamId[pId] ?: emptyList()

                    val consMap = mutableMapOf<String, String>()

                    for (cRec in constraintRows) {
                        val key = cRec.constraintKey
                        if (!key.isNullOrBlank()) {
                            consMap[key] = cRec.constraintValue ?: ""
                        }
                    }

                    domainParams.add(
                        ParameterTask(
                            uid = pRec.uid ?: 0L,
                            name = pRec.name ?: "",
                            type = pRec.type ?: "",
                            unit = pRec.unit ?: "",
                            description = pRec.description ?: "",
                            constraints = consMap
                        )
                    )
                }

                result
                    .getOrPut(deviceId) { mutableListOf() }
                    .add(domainTaskType)
            }

            result
        }
    }

    override fun getId(deviceId: Long, taskTypeUid: Long): Long? {
        return dsl.select(TASK_TYPE.ID)
            .from(TASK_TYPE)
            .where(TASK_TYPE.DEVICE.eq(deviceId))
            .and(TASK_TYPE.UID.eq(taskTypeUid))
            .fetchOne(TASK_TYPE.ID)
    }

    override fun get(device: Device, taskTypeUid: Long): TaskType? {
        return null // TODO implement jOOQ query
    }

    override fun getLazy(device: Device, taskTypeUid: Long): TaskType? {
        return null // TODO implement jOOQ query
    }

    override fun exists(taskTypeUid: Long, deviceId: Long): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(TASK_TYPE)
                .where(TASK_TYPE.DEVICE.eq(deviceId))
                .and(TASK_TYPE.UID.eq(taskTypeUid))
        )
    }

    fun getByDeviceUuid(deviceUuid: String, taskTypeUid: Long): TaskType? {
        val rec = dsl
            .select(
                TASK_TYPE.ID,
                TASK_TYPE.UID,
                TASK_TYPE.NAME,
                TASK_TYPE.DESCRIPTION,
                DEVICE.ID
            )
            .from(TASK_TYPE)
            .join(DEVICE).on(TASK_TYPE.DEVICE.eq(DEVICE.ID))
            .where(DEVICE.UUID.eq(deviceUuid))
            .and(TASK_TYPE.UID.eq(taskTypeUid))
            .fetchOne()
            ?: return null

        return TaskType(
            uid = rec.get(TASK_TYPE.UID)!!,
            name = rec.get(TASK_TYPE.NAME) ?: "",
            description = rec.get(TASK_TYPE.DESCRIPTION) ?: "",
            parameters = emptyList(),
        )
    }
}
