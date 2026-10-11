package com.server.croniot.livelog

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
import com.server.croniot.mqtt.DeviceLogRecord
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset

class LogHistoryDaoIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val dao = LogHistoryDao(dsl)

    private val owner = LogOwnerRef(uuid = "acc-1", nickname = "vladimir", email = "v@example.com")
    private val device = LogDeviceRef(uuid = "dev-1", name = "Riego")

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun at(ms: Long) = Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC)

    // Timeline (ms): 1000 log seq1 (INFO), 2000 event seq1 (ERROR),
    // 3000 server WARN, 4000 log seq2 (WARN), 5000 ingest batch with dups,
    // 6000 server INFO from "Application".
    private fun seed(): Long {
        val accountId = AccountJooqDaoImpl(dsl).insert(
            Account(uuid = owner.uuid, nickname = owner.nickname!!, email = owner.email, devices = mutableListOf()),
            password = "pwd",
        )
        val deviceId = DeviceJooqDaoImpl(dsl).insert(
            DeviceEntity(uuid = device.uuid, name = device.name!!, description = "d", iot = true, accountId = accountId),
        )
        val logs = DeviceLogJooqDaoImpl(dsl).insertBatch(
            deviceId, 9, listOf(
                DeviceLogRecord(1, 100, LogLevels.INFO, "Sensor", "reading ok"),
                DeviceLogRecord(2, 200, LogLevels.WARN, "Uplink", "publish failed"),
            ),
        )
        dsl.update(DEVICE_LOG).set(DEVICE_LOG.RECEIVED_AT, at(1000)).where(DEVICE_LOG.ID.eq(logs[0].id)).execute()
        dsl.update(DEVICE_LOG).set(DEVICE_LOG.RECEIVED_AT, at(4000)).where(DEVICE_LOG.ID.eq(logs[1].id)).execute()

        val events = DeviceEventJooqDaoImpl(dsl).insertBatch(deviceId, 9, listOf(DeviceLogRecord(1, 150, LogLevels.ERROR, "Health", "panic")))
        dsl.update(DEVICE_EVENT).set(DEVICE_EVENT.RECEIVED_AT, at(2000)).where(DEVICE_EVENT.ID.eq(events[0].id)).execute()

        val queue = ServerLogQueue()
        queue.offer("app", serverEntry("srv-x-1", LogLevels.WARN, "MqttDataProcessorDeviceLog", "unknown device", 3000))
        queue.offer("app", serverEntry("srv-x-2", LogLevels.INFO, "Application", "200 OK: POST - /hour", 6000))
        ServerLogDbWriter(dsl, queue, pollTimeoutMs = 10).flushOnce()

        val ingest = IngestBatchJooqDaoImpl(dsl).insert(IngestBatchRow(deviceId, "logs", 9, 1, 2, 2, 0, 2))
        dsl.update(INGEST_BATCH).set(INGEST_BATCH.RECEIVED_AT, at(5000)).where(INGEST_BATCH.ID.eq(ingest.id)).execute()
        return ingest.id
    }

    private fun serverEntry(id: String, level: Int, tag: String, message: String, ms: Long) = LogEntryDto(
        id = id, platform = LogPlatform.SERVER, kind = LogKind.LOG, level = level, tag = tag, message = message,
        receivedAtMs = ms, thread = "t", traceId = null, throwable = null,
    )

    private fun ids(query: LogHistoryQuery = LogHistoryQuery()) = dao.find(query).map { it.id }

    @Test
    fun `WHEN nothing is filtered THEN every source comes back newest first`() {
        val ingestId = seed()

        val entries = dao.find(LogHistoryQuery())

        assertEquals(6, entries.size)
        assertEquals(listOf(6000L, 5000L, 4000L, 3000L, 2000L, 1000L), entries.map { it.receivedAtMs })
        assertEquals("ing-$ingestId", entries[1].id)
        assertEquals("srv-x-1", entries[3].id)

        val event = entries[4]
        assertEquals(LogPlatform.IOT, event.platform)
        assertEquals(LogKind.EVENT, event.kind)
        assertEquals(device, event.device)
        assertEquals(owner, event.owner)
        assertEquals(9L, event.bootId)
        assertEquals(1L, event.seq)
        assertEquals(150L, event.uptimeMs)
    }

    @Test
    fun `WHEN filtering by platform, kind and level THEN only matching entries remain`() {
        seed()

        assertEquals(listOf("srv-x-2", "srv-x-1"), ids(LogHistoryQuery(filter = LiveLogFilter(platforms = setOf(LogPlatform.SERVER)))).filter { it.startsWith("srv") })
        assertEquals(1, dao.find(LogHistoryQuery(filter = LiveLogFilter(kinds = setOf(LogKind.EVENT)))).size)
        assertEquals(
            listOf(4000L, 3000L, 2000L),
            dao.find(LogHistoryQuery(filter = LiveLogFilter(levels = setOf(LogLevels.ERROR, LogLevels.WARN)))).map { it.receivedAtMs },
        )
    }

    @Test
    fun `WHEN filtering by device or owner THEN server lines are excluded`() {
        seed()

        val byDevice = dao.find(LogHistoryQuery(filter = LiveLogFilter(devices = setOf("dev-1"))))
        val byOwner = dao.find(LogHistoryQuery(filter = LiveLogFilter(owners = setOf("acc-1"))))

        assertEquals(4, byDevice.size) // 2 logs, 1 event, 1 ingest line
        assertEquals(byDevice, byOwner)
        assertEquals(0, dao.find(LogHistoryQuery(filter = LiveLogFilter(devices = setOf("other")))).size)
    }

    @Test
    fun `WHEN filtering by tag or text THEN both are applied, text case-insensitively`() {
        seed()

        assertEquals(listOf(4000L), dao.find(LogHistoryQuery(tags = setOf("Uplink"))).map { it.receivedAtMs })
        assertEquals(listOf(4000L), dao.find(LogHistoryQuery(text = "PUBLISH")).map { it.receivedAtMs })
        assertEquals(listOf(6000L), dao.find(LogHistoryQuery(text = "applic")).map { it.receivedAtMs }) // matches the tag
    }

    @Test
    fun `WHEN paging with the cursor THEN pages do not overlap or skip`() {
        seed()

        val first = dao.find(LogHistoryQuery(limit = 4))
        val last = first.last()
        val second = dao.find(LogHistoryQuery(limit = 4, beforeMs = last.receivedAtMs, beforeId = last.id))

        assertEquals(listOf(6000L, 5000L, 4000L, 3000L), first.map { it.receivedAtMs })
        assertEquals(listOf(2000L, 1000L), second.map { it.receivedAtMs })
    }

    @Test
    fun `WHEN asking for a time range THEN only entries inside it, bounds included`() {
        seed()

        assertEquals(listOf(4000L, 3000L, 2000L), dao.find(LogHistoryQuery(fromMs = 2000, toMs = 4000)).map { it.receivedAtMs })
    }

    @Test
    fun `WHEN an ingest batch had duplicates THEN history shows exactly the live line`() {
        val ingestId = seed()
        val stored = com.server.croniot.data.db.daos.StoredIngestBatch(
            id = ingestId,
            row = IngestBatchRow(0, "logs", 9, 1, 2, 2, 0, 2),
            receivedAt = at(5000),
        )

        val fromHistory = dao.find(LogHistoryQuery()).single { it.id == "ing-$ingestId" }

        assertEquals(IngestBatchEntries.toEntry(stored, DeviceContext(device, owner)), fromHistory)
    }
}
