package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.ParameterSensor.Companion.PARAMETER_SENSOR
import com.server.croniot.jooq.tables.ParameterSensorConstraints.Companion.PARAMETER_SENSOR_CONSTRAINTS
import com.server.croniot.jooq.tables.SensorType.Companion.SENSOR_TYPE
import croniot.models.ParameterSensor
import croniot.models.SensorType
import org.jooq.DSLContext
import org.jooq.impl.DSL.using
import javax.inject.Inject

class SensorTypeJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : SensorTypeDao {

    override fun upsert(sensorType: SensorType, deviceId: Long): Long {
        require(deviceId != 0L) { "deviceId (PK interna) es obligatorio." }

        return dsl.transactionResult { cfg ->
            val tx = using(cfg)

            val existingSensorTypeId: Long? = tx
                .select(SENSOR_TYPE.ID)
                .from(SENSOR_TYPE)
                .where(SENSOR_TYPE.UID.eq(sensorType.uid))
                .and(SENSOR_TYPE.DEVICE.eq(deviceId))
                .fetchOne(SENSOR_TYPE.ID)

            val sensorTypeId: Long =
                if (existingSensorTypeId != null) {
                    tx.update(SENSOR_TYPE)
                        .set(SENSOR_TYPE.NAME, sensorType.name)
                        .set(SENSOR_TYPE.DESCRIPTION, sensorType.description)
                        .where(SENSOR_TYPE.ID.eq(existingSensorTypeId))
                        .execute()

                    existingSensorTypeId
                } else {
                    val rec = tx.insertInto(SENSOR_TYPE)
                        .set(SENSOR_TYPE.UID, sensorType.uid)
                        .set(SENSOR_TYPE.NAME, sensorType.name)
                        .set(SENSOR_TYPE.DESCRIPTION, sensorType.description)
                        .set(SENSOR_TYPE.DEVICE, deviceId)
                        .returning(SENSOR_TYPE.ID)
                        .fetchOne()
                        ?: error("INSERT SENSOR_TYPE no devolvio registro")

                    rec[SENSOR_TYPE.ID] ?: error("INSERT SENSOR_TYPE devolvio ID nulo")
                }

            for (p in sensorType.parameters) {
                val existingParamId: Long? = tx
                    .select(PARAMETER_SENSOR.ID)
                    .from(PARAMETER_SENSOR)
                    .where(PARAMETER_SENSOR.UID.eq(p.uid))
                    .and(PARAMETER_SENSOR.SENSOR_TYPE.eq(sensorTypeId))
                    .fetchOne(PARAMETER_SENSOR.ID)

                val parameterId: Long =
                    if (existingParamId != null) {
                        tx.update(PARAMETER_SENSOR)
                            .set(PARAMETER_SENSOR.NAME, p.name)
                            .set(PARAMETER_SENSOR.TYPE, p.type)
                            .set(PARAMETER_SENSOR.UNIT, p.unit)
                            .set(PARAMETER_SENSOR.DESCRIPTION, p.description)
                            .where(PARAMETER_SENSOR.ID.eq(existingParamId))
                            .execute()

                        existingParamId
                    } else {
                        val pRec = tx.insertInto(PARAMETER_SENSOR)
                            .set(PARAMETER_SENSOR.UID, p.uid)
                            .set(PARAMETER_SENSOR.NAME, p.name)
                            .set(PARAMETER_SENSOR.TYPE, p.type)
                            .set(PARAMETER_SENSOR.UNIT, p.unit)
                            .set(PARAMETER_SENSOR.DESCRIPTION, p.description)
                            .set(PARAMETER_SENSOR.SENSOR_TYPE, sensorTypeId)
                            .returning(PARAMETER_SENSOR.ID)
                            .fetchOne()
                            ?: error("INSERT PARAMETER_SENSOR no devolvio registro")

                        pRec[PARAMETER_SENSOR.ID] ?: error("INSERT PARAMETER_SENSOR devolvio ID nulo")
                    }

                tx.deleteFrom(PARAMETER_SENSOR_CONSTRAINTS)
                    .where(PARAMETER_SENSOR_CONSTRAINTS.PARAMETER_ID.eq(parameterId))
                    .execute()

                val cons = p.constraints
                if (cons.isNotEmpty()) {
                    val inserts = cons
                        .filterKeys { it.isNotBlank() }
                        .map { (k, v) ->
                            tx.insertInto(PARAMETER_SENSOR_CONSTRAINTS)
                                .set(PARAMETER_SENSOR_CONSTRAINTS.PARAMETER_ID, parameterId)
                                .set(PARAMETER_SENSOR_CONSTRAINTS.CONSTRAINT_KEY, k)
                                .set(PARAMETER_SENSOR_CONSTRAINTS.CONSTRAINT_VALUE, v)
                        }

                    tx.batch(inserts).execute()
                }
            }

            sensorTypeId
        }
    }

    override fun getByDeviceIds(
        deviceIds: List<Long>
    ): Map<Long, List<SensorType>> {
        if (deviceIds.isEmpty()) return emptyMap()

        return dsl.transactionResult { cfg ->
            val tx = org.jooq.impl.DSL.using(cfg)

            val stRecs = tx
                .selectFrom(SENSOR_TYPE)
                .where(SENSOR_TYPE.DEVICE.`in`(deviceIds))
                .fetch()

            if (stRecs.isEmpty()) return@transactionResult emptyMap()

            val sensorTypeIds = stRecs.mapNotNull { it.id }
            if (sensorTypeIds.isEmpty()) return@transactionResult emptyMap()

            val pRecs = tx
                .selectFrom(PARAMETER_SENSOR)
                .where(PARAMETER_SENSOR.SENSOR_TYPE.`in`(sensorTypeIds))
                .fetch()

            val paramIds = pRecs.mapNotNull { it.id }

            val cRecs =
                if (paramIds.isEmpty()) {
                    emptyList()
                } else {
                    tx.selectFrom(PARAMETER_SENSOR_CONSTRAINTS)
                        .where(PARAMETER_SENSOR_CONSTRAINTS.PARAMETER_ID.`in`(paramIds))
                        .fetch()
                }

            val paramsBySensorTypeId =
                pRecs
                    .filter { it.sensorType != null && it.id != null }
                    .groupBy { it.sensorType!! }

            val constraintsByParamId =
                cRecs
                    .filter { it.parameterId != null }
                    .groupBy { it.parameterId!! }

            val result: MutableMap<Long, MutableList<SensorType>> = mutableMapOf()

            for (stRec in stRecs) {
                val stId = stRec.id ?: continue
                val deviceId = stRec.device ?: continue

                val domainParams = mutableListOf<ParameterSensor>()

                val domainSensorType = SensorType(
                    uid = stRec.uid ?: 0L,
                    name = stRec.name ?: "",
                    description = stRec.description ?: "",
                    parameters = domainParams
                )

                val paramRecsForSt =
                    paramsBySensorTypeId[stId] ?: emptyList()

                for (pRec in paramRecsForSt) {
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
                        ParameterSensor(
                            id = pId,
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
                    .add(domainSensorType)
            }

            result
        }
    }
}
