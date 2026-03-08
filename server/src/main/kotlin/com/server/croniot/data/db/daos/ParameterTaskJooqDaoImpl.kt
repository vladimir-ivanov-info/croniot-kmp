package com.server.croniot.data.db.daos

//package com.server.croniot.data.db.daos

import croniot.models.ParameterTask
import org.jooq.DSLContext
import javax.inject.Inject

import com.server.croniot.jooq.tables.ParameterTask.Companion.PARAMETER_TASK
import com.server.croniot.jooq.tables.ParameterTaskConstraints.Companion.PARAMETER_TASK_CONSTRAINTS

class ParameterTaskDaoJooqImpl @Inject constructor(
    private val dsl: DSLContext,
) : ParameterTaskDao {

    //override fun getByUid(parameterTaskUid: Long, taskType: TaskType): ParameterTask? {
    override fun getByUid(parameterTaskUid: Long, taskTypeId: Long): ParameterTask? {
        require(taskTypeId != 0L) { "getByUid requiere taskType.id (PK interna) != 0" }

        // 1) PARAMETER_TASK (fila principal)
        val rec = dsl
            .select(
                PARAMETER_TASK.ID,
                PARAMETER_TASK.UID,
                PARAMETER_TASK.NAME,
                PARAMETER_TASK.TYPE,
                PARAMETER_TASK.UNIT,
                PARAMETER_TASK.DESCRIPTION,
                PARAMETER_TASK.TASK_TYPE,
            )
            .from(PARAMETER_TASK)
            .where(PARAMETER_TASK.UID.eq(parameterTaskUid))
            .and(PARAMETER_TASK.TASK_TYPE.eq(taskTypeId))
            .fetchOne()
            ?: return null

        val paramId = rec.get(PARAMETER_TASK.ID)!!

        // 2) CONSTRAINTS (tabla hija)
        val constraints: MutableMap<String, String> = dsl
            .select(
                PARAMETER_TASK_CONSTRAINTS.CONSTRAINT_KEY,
                PARAMETER_TASK_CONSTRAINTS.CONSTRAINT_VALUE
            )
            .from(PARAMETER_TASK_CONSTRAINTS)
            .where(PARAMETER_TASK_CONSTRAINTS.PARAMETER_ID.eq(paramId))
            .fetch()
            .associate { r ->
                (r.get(PARAMETER_TASK_CONSTRAINTS.CONSTRAINT_KEY) ?: "") to
                        (r.get(PARAMETER_TASK_CONSTRAINTS.CONSTRAINT_VALUE) ?: "")
            }
            .filterKeys { it.isNotBlank() }
            .toMutableMap()

        // 3) Dominio
        val parameterTask = ParameterTask(
            //id = paramId,
            uid = rec.get(PARAMETER_TASK.UID) ?: 0L,
            name = rec.get(PARAMETER_TASK.NAME) ?: "",
            type = rec.get(PARAMETER_TASK.TYPE) ?: "",
            unit = rec.get(PARAMETER_TASK.UNIT) ?: "",
            description = rec.get(PARAMETER_TASK.DESCRIPTION) ?: "",
            constraints = constraints,
          //  taskTypeId = taskType.id,
           // taskType = null, // lo enlazas arriba si quieres
        )

        return parameterTask
    }
}
