package com.server.croniot.livelog

import com.server.croniot.jooq.tables.Account.Companion.ACCOUNT
import com.server.croniot.jooq.tables.Device.Companion.DEVICE
import com.server.croniot.jooq.tables.DeviceEvent.Companion.DEVICE_EVENT
import com.server.croniot.jooq.tables.DeviceLog.Companion.DEVICE_LOG
import com.server.croniot.jooq.tables.IngestBatch.Companion.INGEST_BATCH
import com.server.croniot.jooq.tables.ServerLog.Companion.SERVER_LOG
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Table
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.DSL.castNull
import org.jooq.impl.DSL.inline
import org.jooq.impl.SQLDataType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

// The admin viewer's history: device_log, device_event, server_log and the
// ingest_batch rows that had duplicates, as one newest-first list of
// LogEntryDto. Each table is projected onto the same columns and the four
// are UNION ALL'd; the filters go on the outer query (Postgres pushes them
// down, and a platform/kind filter that a branch cannot satisfy prunes
// that whole branch).
//
// Ids are the same strings the live stream uses ("log-12", "evt-7",
// "ing-5", and server_log.entry_id), so the viewer can merge both without
// duplicates. Ties on received_at are broken by id in "C" collation, so
// the keyset cursor is deterministic.
class LogHistoryDao @Inject constructor(
    private val dsl: DSLContext,
) {
    // device_log and device_event have the same columns; this names them
    // once so both branches are built by the same code.
    private class DeviceTable(
        val table: Table<*>,
        val prefix: String,
        val kind: LogKind,
        val id: TableField<*, Long?>,
        val device: TableField<*, Long?>,
        val level: TableField<*, Short?>,
        val tag: TableField<*, String?>,
        val message: TableField<*, String?>,
        val receivedAt: TableField<*, OffsetDateTime?>,
        val bootId: TableField<*, Long?>,
        val seq: TableField<*, Long?>,
        val uptimeMs: TableField<*, Long?>,
    )

    private val logs = with(DEVICE_LOG) {
        DeviceTable(this, "log", LogKind.LOG, ID, DEVICE, LEVEL, TAG, MESSAGE, RECEIVED_AT, BOOT_ID, SEQ, UPTIME_MS)
    }
    private val events = with(DEVICE_EVENT) {
        DeviceTable(this, "evt", LogKind.EVENT, ID, DEVICE, LEVEL, TAG, MESSAGE, RECEIVED_AT, BOOT_ID, SEQ, UPTIME_MS)
    }

    fun find(query: LogHistoryQuery): List<LogEntryDto> {
        val union = deviceRows(logs)
            .unionAll(deviceRows(events))
            .unionAll(ingestRows())
            .unionAll(serverRows())
            .asTable("e")

        return dsl.select(union.fields().toList())
            .from(union)
            .where(conditions(query))
            .orderBy(RECEIVED_AT.desc(), ENTRY_ID.collate("C").desc())
            .limit(query.limit)
            .fetch()
            .map(::toEntry)
    }

    private fun deviceRows(t: DeviceTable) = dsl.select(
        DSL.concat(inline("${t.prefix}-"), t.id.cast(SQLDataType.VARCHAR)).`as`(ENTRY_ID.name),
        inline(LogPlatform.IOT.name).`as`(PLATFORM.name),
        inline(t.kind.name).`as`(KIND.name),
        t.level.cast(SQLDataType.INTEGER).`as`(LEVEL.name),
        t.tag.cast(SQLDataType.VARCHAR).`as`(TAG.name),
        t.message.cast(SQLDataType.CLOB).`as`(MESSAGE.name),
        t.receivedAt.`as`(RECEIVED_AT.name),
        DEVICE.UUID.`as`(DEVICE_UUID.name),
        DEVICE.NAME.`as`(DEVICE_NAME.name),
        ACCOUNT.UUID.`as`(OWNER_UUID.name),
        ACCOUNT.NICKNAME.`as`(OWNER_NICKNAME.name),
        ACCOUNT.EMAIL.`as`(OWNER_EMAIL.name),
        t.bootId.`as`(BOOT_ID.name),
        t.seq.`as`(SEQ.name),
        t.uptimeMs.`as`(UPTIME_MS.name),
        castNull(SQLDataType.VARCHAR).`as`(THREAD.name),
        castNull(SQLDataType.VARCHAR).`as`(TRACE_ID.name),
        castNull(SQLDataType.CLOB).`as`(THROWABLE.name),
    )
        .from(t.table)
        .join(DEVICE).on(DEVICE.ID.eq(t.device))
        .join(ACCOUNT).on(ACCOUNT.ID.eq(DEVICE.ACCOUNT))

    // Must produce exactly IngestBatchEntries' message for the same row
    // (the live line); LogHistoryDaoIntegrationTest checks they match.
    private fun ingestRows() = dsl.select(
        DSL.concat(inline("ing-"), INGEST_BATCH.ID.cast(SQLDataType.VARCHAR)).`as`(ENTRY_ID.name),
        inline(LogPlatform.SERVER.name).`as`(PLATFORM.name),
        inline(LogKind.LOG.name).`as`(KIND.name),
        inline(LogLevels.INFO).`as`(LEVEL.name),
        inline(IngestBatchEntries.TAG).cast(SQLDataType.VARCHAR).`as`(TAG.name),
        DSL.concat(
            inline("Resent batch: "), INGEST_BATCH.DUPLICATED.cast(SQLDataType.VARCHAR),
            inline("/"), INGEST_BATCH.RECORDS.cast(SQLDataType.VARCHAR),
            inline(" duplicate records (stream="), INGEST_BATCH.STREAM,
            inline(", boot="), INGEST_BATCH.BOOT_ID.cast(SQLDataType.VARCHAR),
            inline(", seq "), INGEST_BATCH.FIRST_SEQ.cast(SQLDataType.VARCHAR),
            DSL.`when`(INGEST_BATCH.FIRST_SEQ.eq(INGEST_BATCH.LAST_SEQ), inline(""))
                .otherwise(DSL.concat(inline("-"), INGEST_BATCH.LAST_SEQ.cast(SQLDataType.VARCHAR))),
            inline(")"),
        ).cast(SQLDataType.CLOB).`as`(MESSAGE.name),
        INGEST_BATCH.RECEIVED_AT.`as`(RECEIVED_AT.name),
        DEVICE.UUID.`as`(DEVICE_UUID.name),
        DEVICE.NAME.`as`(DEVICE_NAME.name),
        ACCOUNT.UUID.`as`(OWNER_UUID.name),
        ACCOUNT.NICKNAME.`as`(OWNER_NICKNAME.name),
        ACCOUNT.EMAIL.`as`(OWNER_EMAIL.name),
        INGEST_BATCH.BOOT_ID.`as`(BOOT_ID.name),
        castNull(SQLDataType.BIGINT).`as`(SEQ.name),
        castNull(SQLDataType.BIGINT).`as`(UPTIME_MS.name),
        castNull(SQLDataType.VARCHAR).`as`(THREAD.name),
        castNull(SQLDataType.VARCHAR).`as`(TRACE_ID.name),
        castNull(SQLDataType.CLOB).`as`(THROWABLE.name),
    )
        .from(INGEST_BATCH)
        .join(DEVICE).on(DEVICE.ID.eq(INGEST_BATCH.DEVICE))
        .join(ACCOUNT).on(ACCOUNT.ID.eq(DEVICE.ACCOUNT))
        .where(INGEST_BATCH.DUPLICATED.gt(0))

    private fun serverRows() = dsl.select(
        SERVER_LOG.ENTRY_ID.`as`(ENTRY_ID.name),
        inline(LogPlatform.SERVER.name).`as`(PLATFORM.name),
        inline(LogKind.LOG.name).`as`(KIND.name),
        SERVER_LOG.LEVEL.cast(SQLDataType.INTEGER).`as`(LEVEL.name),
        SERVER_LOG.TAG.`as`(TAG.name),
        SERVER_LOG.MESSAGE.cast(SQLDataType.CLOB).`as`(MESSAGE.name),
        SERVER_LOG.CREATED_AT.`as`(RECEIVED_AT.name),
        castNull(SQLDataType.VARCHAR).`as`(DEVICE_UUID.name),
        castNull(SQLDataType.VARCHAR).`as`(DEVICE_NAME.name),
        castNull(SQLDataType.VARCHAR).`as`(OWNER_UUID.name),
        castNull(SQLDataType.VARCHAR).`as`(OWNER_NICKNAME.name),
        castNull(SQLDataType.VARCHAR).`as`(OWNER_EMAIL.name),
        castNull(SQLDataType.BIGINT).`as`(BOOT_ID.name),
        castNull(SQLDataType.BIGINT).`as`(SEQ.name),
        castNull(SQLDataType.BIGINT).`as`(UPTIME_MS.name),
        SERVER_LOG.THREAD.`as`(THREAD.name),
        SERVER_LOG.TRACE_ID.`as`(TRACE_ID.name),
        SERVER_LOG.THROWABLE.cast(SQLDataType.CLOB).`as`(THROWABLE.name),
    ).from(SERVER_LOG)

    private fun conditions(query: LogHistoryQuery): List<Condition> {
        val f = query.filter
        val conditions = mutableListOf<Condition>()
        if (f.platforms.isNotEmpty()) conditions += PLATFORM.`in`(f.platforms.map { it.name })
        if (f.kinds.isNotEmpty()) conditions += KIND.`in`(f.kinds.map { it.name })
        if (f.levels.isNotEmpty()) conditions += LEVEL.`in`(f.levels)
        if (f.devices.isNotEmpty()) conditions += DEVICE_UUID.`in`(f.devices)
        if (f.owners.isNotEmpty()) conditions += OWNER_UUID.`in`(f.owners)
        if (query.tags.isNotEmpty()) conditions += TAG.`in`(query.tags)
        query.text?.let { conditions += MESSAGE.containsIgnoreCase(it).or(TAG.containsIgnoreCase(it)) }
        query.fromMs?.let { conditions += RECEIVED_AT.ge(at(it)) }
        query.toMs?.let { conditions += RECEIVED_AT.le(at(it)) }
        query.beforeMs?.let { before ->
            val ts = at(before)
            conditions += if (query.beforeId == null) {
                RECEIVED_AT.lt(ts)
            } else {
                RECEIVED_AT.lt(ts).or(RECEIVED_AT.eq(ts).and(ENTRY_ID.collate("C").lt(inline(query.beforeId).collate("C"))))
            }
        }
        return conditions
    }

    private fun toEntry(r: Record): LogEntryDto {
        val deviceUuid = r.get(DEVICE_UUID)
        val ownerUuid = r.get(OWNER_UUID)
        return LogEntryDto(
            id = r.get(ENTRY_ID)!!,
            platform = LogPlatform.valueOf(r.get(PLATFORM)!!),
            kind = LogKind.valueOf(r.get(KIND)!!),
            level = r.get(LEVEL)!!,
            tag = r.get(TAG).orEmpty(),
            message = r.get(MESSAGE).orEmpty(),
            receivedAtMs = r.get(RECEIVED_AT)!!.toInstant().toEpochMilli(),
            device = deviceUuid?.let { LogDeviceRef(uuid = it, name = r.get(DEVICE_NAME)) },
            owner = ownerUuid?.let { LogOwnerRef(uuid = it, nickname = r.get(OWNER_NICKNAME), email = r.get(OWNER_EMAIL).orEmpty()) },
            bootId = r.get(BOOT_ID),
            seq = r.get(SEQ),
            uptimeMs = r.get(UPTIME_MS),
            thread = r.get(THREAD),
            traceId = r.get(TRACE_ID),
            throwable = r.get(THROWABLE),
        )
    }

    private fun at(ms: Long): OffsetDateTime = Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC)

    private companion object {
        val ENTRY_ID: Field<String?> = DSL.field(DSL.name("entry_id"), SQLDataType.VARCHAR)
        val PLATFORM: Field<String?> = DSL.field(DSL.name("platform"), SQLDataType.VARCHAR)
        val KIND: Field<String?> = DSL.field(DSL.name("kind"), SQLDataType.VARCHAR)
        val LEVEL: Field<Int?> = DSL.field(DSL.name("level"), SQLDataType.INTEGER)
        val TAG: Field<String?> = DSL.field(DSL.name("tag"), SQLDataType.VARCHAR)
        val MESSAGE: Field<String?> = DSL.field(DSL.name("message"), SQLDataType.CLOB)
        val RECEIVED_AT: Field<OffsetDateTime?> = DSL.field(DSL.name("received_at"), SQLDataType.TIMESTAMPWITHTIMEZONE)
        val DEVICE_UUID: Field<String?> = DSL.field(DSL.name("device_uuid"), SQLDataType.VARCHAR)
        val DEVICE_NAME: Field<String?> = DSL.field(DSL.name("device_name"), SQLDataType.VARCHAR)
        val OWNER_UUID: Field<String?> = DSL.field(DSL.name("owner_uuid"), SQLDataType.VARCHAR)
        val OWNER_NICKNAME: Field<String?> = DSL.field(DSL.name("owner_nickname"), SQLDataType.VARCHAR)
        val OWNER_EMAIL: Field<String?> = DSL.field(DSL.name("owner_email"), SQLDataType.VARCHAR)
        val BOOT_ID: Field<Long?> = DSL.field(DSL.name("boot_id"), SQLDataType.BIGINT)
        val SEQ: Field<Long?> = DSL.field(DSL.name("seq"), SQLDataType.BIGINT)
        val UPTIME_MS: Field<Long?> = DSL.field(DSL.name("uptime_ms"), SQLDataType.BIGINT)
        val THREAD: Field<String?> = DSL.field(DSL.name("thread"), SQLDataType.VARCHAR)
        val TRACE_ID: Field<String?> = DSL.field(DSL.name("trace_id"), SQLDataType.VARCHAR)
        val THROWABLE: Field<String?> = DSL.field(DSL.name("throwable"), SQLDataType.CLOB)
    }
}
