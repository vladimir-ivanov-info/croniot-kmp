package com.server.croniot.services

import com.server.croniot.data.db.daos.AccountJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceEventJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceLogJooqDaoImpl
import com.server.croniot.data.db.daos.IngestBatchJooqDaoImpl
import com.server.croniot.data.db.daos.IngestBatchRow
import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.jooq.tables.DeviceEvent.Companion.DEVICE_EVENT
import com.server.croniot.jooq.tables.DeviceLog.Companion.DEVICE_LOG
import com.server.croniot.jooq.tables.IngestBatch.Companion.INGEST_BATCH
import com.server.croniot.jooq.tables.ServerLog.Companion.SERVER_LOG
import com.server.croniot.mqtt.DeviceLogRecord
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.OffsetDateTime

class LogRetentionServiceIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun daysAgo(days: Long) = OffsetDateTime.now().minusDays(days)

    private fun seedDevice(): Long {
        val accountId = AccountJooqDaoImpl(dsl).insert(
            Account(uuid = "acc", nickname = "n", email = "a@example.com", devices = mutableListOf()),
            password = "pwd",
        )
        return DeviceJooqDaoImpl(dsl).insert(DeviceEntity(uuid = "dev", name = "n", description = "d", iot = true, accountId = accountId))
    }

    // One row "days" old in every table.
    private fun seedRowsAged(deviceId: Long, seq: Long, days: Long) {
        val log = DeviceLogJooqDaoImpl(dsl).insertBatch(deviceId, 1, listOf(DeviceLogRecord(seq, 0, 2, "T", "m"))).single()
        dsl.update(DEVICE_LOG).set(DEVICE_LOG.RECEIVED_AT, daysAgo(days)).where(DEVICE_LOG.ID.eq(log.id)).execute()
        val event = DeviceEventJooqDaoImpl(dsl).insertBatch(deviceId, 1, listOf(DeviceLogRecord(seq, 0, 2, "T", "m"))).single()
        dsl.update(DEVICE_EVENT).set(DEVICE_EVENT.RECEIVED_AT, daysAgo(days)).where(DEVICE_EVENT.ID.eq(event.id)).execute()
        val ingest = IngestBatchJooqDaoImpl(dsl).insert(IngestBatchRow(deviceId, "logs", 1, seq, seq, 1, 1, 0))
        dsl.update(INGEST_BATCH).set(INGEST_BATCH.RECEIVED_AT, daysAgo(days)).where(INGEST_BATCH.ID.eq(ingest.id)).execute()
        dsl.insertInto(SERVER_LOG)
            .set(SERVER_LOG.ENTRY_ID, "srv-$seq")
            .set(SERVER_LOG.LEVEL, 2.toShort())
            .set(SERVER_LOG.TAG, "T")
            .set(SERVER_LOG.MESSAGE, "m")
            .set(SERVER_LOG.CREATED_AT, daysAgo(days))
            .execute()
    }

    private fun counts() = listOf(DEVICE_LOG, DEVICE_EVENT, SERVER_LOG, INGEST_BATCH).map { dsl.fetchCount(it) }

    @Test
    fun `WHEN rows are older than their table's retention THEN only those are deleted`() {
        val deviceId = seedDevice()
        seedRowsAged(deviceId, seq = 1, days = 1) // recent everywhere
        seedRowsAged(deviceId, seq = 2, days = 20) // old for logs (14), kept for events/server/ingest
        seedRowsAged(deviceId, seq = 3, days = 200) // old everywhere

        val deleted = LogRetentionService(dsl, LogRetentionPolicy(), Clock.systemUTC()).runOnce()

        assertEquals(mapOf("device_log" to 2, "device_event" to 1, "server_log" to 1, "ingest_batch" to 1), deleted)
        assertEquals(listOf(1, 2, 2, 2), counts())
    }

    @Test
    fun `WHEN there are more old rows than one chunk THEN they are all deleted`() {
        val deviceId = seedDevice()
        val records = (1L..(LogRetentionService.CHUNK_SIZE + 10L)).map { DeviceLogRecord(it, 0, 2, "T", "m") }
        DeviceLogJooqDaoImpl(dsl).insertBatch(deviceId, 1, records)
        dsl.update(DEVICE_LOG).set(DEVICE_LOG.RECEIVED_AT, daysAgo(30)).where(DSL.trueCondition()).execute()

        val deleted = LogRetentionService(dsl, LogRetentionPolicy(), Clock.systemUTC()).runOnce()

        assertEquals(LogRetentionService.CHUNK_SIZE + 10, deleted["device_log"])
        assertEquals(0, dsl.fetchCount(DEVICE_LOG))
    }

    @Test
    fun `WHEN environment variables are set THEN they override the defaults, ignoring invalid ones`() {
        val policy = LogRetentionPolicy.fromEnvironment(
            mapOf("CRONIOT_RETENTION_DEVICE_LOG_DAYS" to "3", "CRONIOT_RETENTION_SERVER_LOG_DAYS" to "nope"),
        )

        assertEquals(LogRetentionPolicy(deviceLogDays = 3), policy)
    }
}
