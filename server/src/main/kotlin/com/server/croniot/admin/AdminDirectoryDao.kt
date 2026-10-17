package com.server.croniot.admin

import com.server.croniot.jooq.tables.Account.Companion.ACCOUNT
import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.DeviceEvent.Companion.DEVICE_EVENT
import com.server.croniot.jooq.tables.DeviceLog.Companion.DEVICE_LOG
import com.server.croniot.jooq.tables.DeviceStatus.Companion.DEVICE_STATUS
import com.server.croniot.jooq.tables.IngestBatch.Companion.INGEST_BATCH
import com.server.croniot.livelog.LogLevels
import com.server.croniot.livelog.LogOwnerRef
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

// Read-only queries behind the admin device and owner pages. Everything
// "recent" means the last 24 h before now (the Clock is injectable for
// tests). The per-device subqueries use the (device, received_at)
// indexes, so the cost grows with the number of devices, not of logs.
class AdminDirectoryDao(
    private val dsl: DSLContext,
    private val clock: Clock,
) {
    @Inject constructor(dsl: DSLContext) : this(dsl, Clock.systemUTC())

    fun listDevices(): List<AdminDeviceSummaryDto> = summaries(DSL.noCondition())

    fun deviceDetail(deviceUuid: String): AdminDeviceDetailDto? {
        val deviceId = dsl.select(DEVICE.ID).from(DEVICE).where(DEVICE.UUID.eq(deviceUuid)).fetchOne(DEVICE.ID)
            ?: return null
        val summary = summaries(DEVICE.ID.eq(deviceId)).singleOrNull() ?: return null
        val since = since()

        val level = DSL.field(DSL.name("level"), Short::class.java)
        val levels = DSL.select(DEVICE_LOG.LEVEL.`as`("level")).from(DEVICE_LOG)
            .where(DEVICE_LOG.DEVICE.eq(deviceId), DEVICE_LOG.RECEIVED_AT.ge(since))
            .unionAll(
                DSL.select(DEVICE_EVENT.LEVEL.`as`("level")).from(DEVICE_EVENT)
                    .where(DEVICE_EVENT.DEVICE.eq(deviceId), DEVICE_EVENT.RECEIVED_AT.ge(since)),
            )
            .asTable("l")
        val levelCounts = dsl.select(level, DSL.count())
            .from(levels)
            .groupBy(level)
            .fetch()
            .associate { (it.value1() ?: 0).toInt() to it.value2().toLong() }

        val ingest = dsl.select(DSL.sum(INGEST_BATCH.RECORDS), DSL.sum(INGEST_BATCH.DUPLICATED))
            .from(INGEST_BATCH)
            .where(INGEST_BATCH.DEVICE.eq(deviceId), INGEST_BATCH.RECEIVED_AT.ge(since))
            .fetchOne()

        val description = dsl.select(
            DEVICE.DESCRIPTION
        ).from(DEVICE).where(DEVICE.ID.eq(deviceId)).fetchOne(DEVICE.DESCRIPTION)

        return AdminDeviceDetailDto(
            summary = summary,
            description = description,
            levelCounts24h = levelCounts,
            recordsReceived24h = ingest?.value1()?.toLong() ?: 0,
            recordsDuplicated24h = ingest?.value2()?.toLong() ?: 0,
        )
    }

    fun accountDetail(accountUuid: String): AdminAccountDetailDto? {
        val owner = dsl.select(ACCOUNT.UUID, ACCOUNT.NICKNAME, ACCOUNT.EMAIL)
            .from(ACCOUNT)
            .where(ACCOUNT.UUID.eq(accountUuid))
            .fetchOne { LogOwnerRef(uuid = it.value1() ?: "", nickname = it.value2(), email = it.value3() ?: "") }
            ?: return null
        return AdminAccountDetailDto(owner = owner, devices = summaries(ACCOUNT.UUID.eq(accountUuid)))
    }

    private fun summaries(condition: Condition): List<AdminDeviceSummaryDto> {
        val since = since()
        val lastLog = DSL.field(
            DSL.select(DSL.max(DEVICE_LOG.RECEIVED_AT)).from(DEVICE_LOG).where(DEVICE_LOG.DEVICE.eq(DEVICE.ID)),
        )
        val lastEvent = DSL.field(
            DSL.select(DSL.max(DEVICE_EVENT.RECEIVED_AT)).from(DEVICE_EVENT).where(DEVICE_EVENT.DEVICE.eq(DEVICE.ID)),
        )
        val logErrors = DSL.field(
            DSL.selectCount().from(DEVICE_LOG).where(
                DEVICE_LOG.DEVICE.eq(DEVICE.ID),
                DEVICE_LOG.RECEIVED_AT.ge(since),
                DEVICE_LOG.LEVEL.eq(LogLevels.ERROR.toShort()),
            ),
        )
        val eventErrors = DSL.field(
            DSL.selectCount().from(DEVICE_EVENT).where(
                DEVICE_EVENT.DEVICE.eq(DEVICE.ID),
                DEVICE_EVENT.RECEIVED_AT.ge(since),
                DEVICE_EVENT.LEVEL.eq(LogLevels.ERROR.toShort()),
            ),
        )
        val lastActivity: Field<OffsetDateTime?> = DSL.greatest(lastLog, lastEvent).`as`("last_activity")
        val errors = logErrors.plus(eventErrors).`as`("errors_24h")

        return dsl.select(
            DEVICE.UUID, DEVICE.NAME, ACCOUNT.UUID, ACCOUNT.NICKNAME, ACCOUNT.EMAIL,
            DEVICE_STATUS.ONLINE, DEVICE_STATUS.BOOT_ID, DEVICE_STATUS.UPDATED_AT,
            lastActivity, errors,
        )
            .from(DEVICE)
            .join(ACCOUNT).on(ACCOUNT.ID.eq(DEVICE.ACCOUNT))
            .leftJoin(DEVICE_STATUS).on(DEVICE_STATUS.DEVICE.eq(DEVICE.ID))
            .where(condition)
            .orderBy(ACCOUNT.EMAIL, DEVICE.NAME, DEVICE.UUID)
            .fetch { r -> toSummary(r, lastActivity, errors) }
    }

    private fun toSummary(r: Record, lastActivity: Field<OffsetDateTime?>, errors: Field<Int>) = AdminDeviceSummaryDto(
        uuid = r.get(DEVICE.UUID) ?: "",
        name = r.get(DEVICE.NAME),
        owner = LogOwnerRef(
            uuid = r.get(ACCOUNT.UUID) ?: "",
            nickname = r.get(ACCOUNT.NICKNAME),
            email = r.get(ACCOUNT.EMAIL) ?: ""
        ),
        online = r.get(DEVICE_STATUS.ONLINE),
        bootId = r.get(DEVICE_STATUS.BOOT_ID),
        statusChangedAtMs = r.get(DEVICE_STATUS.UPDATED_AT)?.toInstant()?.toEpochMilli(),
        lastActivityMs = r.get(lastActivity)?.toInstant()?.toEpochMilli(),
        errors24h = (r.get(errors) ?: 0).toLong(),
    )

    private fun since(): OffsetDateTime = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).minusHours(24)
}
