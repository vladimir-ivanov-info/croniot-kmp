package com.server.croniot.data.db.daos

// package com.server.croniot.data.db.daos

import croniot.models.TaskStateInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import java.time.ZoneId
import javax.inject.Inject

// imports jOOQ codegen (AJUSTA nombres si difieren)
import com.server.croniot.jooq.tables.Task.Companion.TASK
import com.server.croniot.jooq.tables.TaskStateInfo.Companion.TASK_STATE_INFO

class TaskStateInfoDaoJooqImpl @Inject constructor(
    private val dsl: DSLContext,
) : TaskStateInfoDao {

    /**
     * Inserta un TaskStateInfo para un Task existente (taskId = PK interna en BD).
     *
     * Importante: en jOOQ no hay "managed entities" ni cascades.
     * Hay que insertar con FK explícita.
     */
    override fun insert(taskStateInfo: TaskStateInfo, taskId: Long): Long {
        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            // (Opcional) Validación rápida: comprobar que existe el taskId
            val exists = tx.fetchExists(
                tx.selectOne()
                    .from(TASK)
                    .where(TASK.ID.eq(taskId))
            )
            if (!exists) {
                throw IllegalArgumentException("Task id=$taskId no existe")
            }

            // ZonedDateTime -> OffsetDateTime (jOOQ suele mapear TIMESTAMPTZ a OffsetDateTime)
            val odt = taskStateInfo.dateTime
                .withZoneSameInstant(ZoneId.systemDefault())
                .toOffsetDateTime()

            tx.insertInto(TASK_STATE_INFO)
                .set(TASK_STATE_INFO.TASK, taskId) // o TASK_ID según tu schema
                .set(TASK_STATE_INFO.DATE_TIME, odt)
                .set(TASK_STATE_INFO.STATE, taskStateInfo.state)
                .set(TASK_STATE_INFO.PROGRESS, taskStateInfo.progress)
                .set(TASK_STATE_INFO.ERROR_MESSAGE, taskStateInfo.errorMessage)
                .returning(TASK_STATE_INFO.ID)
                .fetchOne()!!
                .get(TASK_STATE_INFO.ID)!!
        }
    }
}
