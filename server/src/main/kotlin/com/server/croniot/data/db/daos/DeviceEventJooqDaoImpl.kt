package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.DeviceEvent.Companion.DEVICE_EVENT
import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import org.jooq.DSLContext
import java.time.OffsetDateTime
import javax.inject.Inject

class DeviceEventJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : DeviceEventDao {

    override fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): Int {
        if (records.isEmpty()) return 0

        val step = dsl.insertInto(
            DEVICE_EVENT,
            DEVICE_EVENT.DEVICE, DEVICE_EVENT.BOOT_ID, DEVICE_EVENT.SEQ,
            DEVICE_EVENT.UPTIME_MS, DEVICE_EVENT.LEVEL, DEVICE_EVENT.TAG, DEVICE_EVENT.MESSAGE,
        )
        for (r in records) {
            step.values(deviceId, bootId, r.seq, r.uptimeMs, r.level.toShort(), r.tag, r.message)
        }
        return step.onConflictDoNothing().execute()
    }

    override fun getRecent(
        deviceId: Long,
        limit: Int,
        before: OffsetDateTime?,
        beforeId: Long?,
        minLevel: Int?,
    ): List<DeviceLogEntryDto> {
        var condition = DEVICE_EVENT.DEVICE.eq(deviceId)
        if (minLevel != null) {
            condition = condition.and(DEVICE_EVENT.LEVEL.le(minLevel.toShort()))
        }
        if (before != null) {
            val effectiveBeforeId = beforeId ?: Long.MAX_VALUE
            condition = condition.and(
                DEVICE_EVENT.RECEIVED_AT.lt(before)
                    .or(DEVICE_EVENT.RECEIVED_AT.eq(before).and(DEVICE_EVENT.ID.lt(effectiveBeforeId)))
            )
        }

        return dsl.selectFrom(DEVICE_EVENT)
            .where(condition)
            .orderBy(DEVICE_EVENT.RECEIVED_AT.desc(), DEVICE_EVENT.ID.desc())
            .limit(limit)
            .fetch()
            .map { rec ->
                DeviceLogEntryDto(
                    id = rec.id ?: 0L,
                    bootId = rec.bootId ?: 0L,
                    seq = rec.seq ?: 0L,
                    uptimeMs = rec.uptimeMs ?: 0L,
                    level = rec.level?.toInt() ?: 0,
                    tag = rec.tag ?: "",
                    message = rec.message ?: "",
                    receivedAt = (rec.receivedAt ?: OffsetDateTime.now()).toZonedDateTime(),
                )
            }
    }
}
