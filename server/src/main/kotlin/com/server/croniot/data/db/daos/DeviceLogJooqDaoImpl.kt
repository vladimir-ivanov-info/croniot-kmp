package com.server.croniot.data.db.daos

import com.server.croniot.jooq.tables.DeviceLog.Companion.DEVICE_LOG
import com.server.croniot.mqtt.DeviceLogRecord
import croniot.models.dto.DeviceLogEntryDto
import org.jooq.DSLContext
import java.time.OffsetDateTime
import javax.inject.Inject

class DeviceLogJooqDaoImpl @Inject constructor(
    private val dsl: DSLContext,
) : DeviceLogDao {

    override fun insertBatch(deviceId: Long, bootId: Long, records: List<DeviceLogRecord>): Int {
        if (records.isEmpty()) return 0

        val step = dsl.insertInto(
            DEVICE_LOG,
            DEVICE_LOG.DEVICE, DEVICE_LOG.BOOT_ID, DEVICE_LOG.SEQ,
            DEVICE_LOG.UPTIME_MS, DEVICE_LOG.LEVEL, DEVICE_LOG.TAG, DEVICE_LOG.MESSAGE,
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
        var condition = DEVICE_LOG.DEVICE.eq(deviceId)
        if (minLevel != null) {
            // meetsThreshold semantics (croniot-iot's Level.h): lower
            // number = more severe, a record qualifies if its level is
            // at least as severe as the requested threshold.
            condition = condition.and(DEVICE_LOG.LEVEL.le(minLevel.toShort()))
        }
        if (before != null) {
            val effectiveBeforeId = beforeId ?: Long.MAX_VALUE
            condition = condition.and(
                DEVICE_LOG.RECEIVED_AT.lt(before)
                    .or(DEVICE_LOG.RECEIVED_AT.eq(before).and(DEVICE_LOG.ID.lt(effectiveBeforeId)))
            )
        }

        return dsl.selectFrom(DEVICE_LOG)
            .where(condition)
            .orderBy(DEVICE_LOG.RECEIVED_AT.desc(), DEVICE_LOG.ID.desc())
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
