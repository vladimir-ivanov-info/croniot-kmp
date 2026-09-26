package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.SensorData.Companion.SENSOR_DATA
import org.jooq.DSLContext
import java.time.OffsetDateTime
import javax.inject.Inject

class SensorDataJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : SensorDataDao {

    override fun insertBatch(
        deviceId: Long,
        sensorTypeId: Long,
        bootId: Long,
        seq: Long,
        values: List<Double>,
        receivedAt: OffsetDateTime,
    ): Int {
        if (values.isEmpty()) return 0

        val step = dsl.insertInto(
            SENSOR_DATA,
            SENSOR_DATA.DEVICE, SENSOR_DATA.SENSORTYPE, SENSOR_DATA.VALUE, SENSOR_DATA.DATE_TIME,
            SENSOR_DATA.BOOT_ID, SENSOR_DATA.SEQ, SENSOR_DATA.SAMPLE_INDEX,
        )
        values.forEachIndexed { index, value ->
            // Stored as a string, same as the legacy per-reading path
            // (sensor_data.value is VARCHAR) - the plan's own "hoy se
            // envían como string" gap (§7.1) isn't closed by this PR;
            // changing that column's type would also affect the
            // untouched legacy path, a separate concern from batching.
            step.values(deviceId, sensorTypeId, value.toString(), receivedAt, bootId, seq, index)
        }
        return step.onConflictDoNothing().execute()
    }
}
