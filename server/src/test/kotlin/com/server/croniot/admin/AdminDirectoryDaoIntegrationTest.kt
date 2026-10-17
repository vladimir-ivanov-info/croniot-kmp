package com.server.croniot.admin

import com.server.croniot.data.db.daos.AccountJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceEventJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceLogJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceStatusDao
import com.server.croniot.data.db.daos.IngestBatchJooqDaoImpl
import com.server.croniot.data.db.daos.IngestBatchRow
import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.jooq.tables.DeviceLog.Companion.DEVICE_LOG
import com.server.croniot.livelog.LogLevels
import com.server.croniot.livelog.LogOwnerRef
import com.server.croniot.mqtt.DeviceLogRecord
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class AdminDirectoryDaoIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val dao = AdminDirectoryDao(dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun account(uuid: String, email: String) = AccountJooqDaoImpl(dsl).insert(
        Account(uuid = uuid, nickname = "nick-$uuid", email = email, devices = mutableListOf()),
        password = "pwd",
    )

    private fun device(uuid: String, name: String, accountId: Long) =
        DeviceJooqDaoImpl(
            dsl
        ).insert(DeviceEntity(uuid = uuid, name = name, description = "desc $uuid", iot = true, accountId = accountId))

    private fun record(seq: Long, level: Int) = DeviceLogRecord(seq, seq * 10, level, "T", "m$seq")

    @Test
    fun `WHEN a device has activity THEN its page shows status, counts per level and duplicate ratio`() {
        val acc = account("acc-1", "v@example.com")
        val dev = device("dev-1", "Riego", acc)
        DeviceLogJooqDaoImpl(
            dsl
        ).insertBatch(dev, 5, listOf(record(1, LogLevels.ERROR), record(2, LogLevels.INFO), record(3, LogLevels.INFO)))
        DeviceEventJooqDaoImpl(dsl).insertBatch(dev, 5, listOf(record(1, LogLevels.ERROR)))
        // An old error, outside the 24 h window: must not count.
        val old = DeviceLogJooqDaoImpl(dsl).insertBatch(dev, 4, listOf(record(9, LogLevels.ERROR))).single()
        dsl.update(
            DEVICE_LOG
        ).set(DEVICE_LOG.RECEIVED_AT, OffsetDateTime.now().minusDays(3)).where(DEVICE_LOG.ID.eq(old.id)).execute()
        DeviceStatusDao(dsl).upsert(dev, online = true, bootId = 5)
        IngestBatchJooqDaoImpl(dsl).insert(IngestBatchRow(dev, "logs", 5, 1, 3, 3, 3, 0))
        IngestBatchJooqDaoImpl(dsl).insert(IngestBatchRow(dev, "logs", 5, 1, 3, 3, 0, 3))

        val detail = dao.deviceDetail("dev-1")

        assertNotNull(detail)
        val summary = detail!!.summary
        assertEquals("Riego", summary.name)
        assertEquals(LogOwnerRef("acc-1", "nick-acc-1", "v@example.com"), summary.owner)
        assertEquals(true, summary.online)
        assertEquals(5L, summary.bootId)
        assertEquals(2L, summary.errors24h) // 1 log + 1 event, not the old one
        assertNotNull(summary.lastActivityMs)
        assertEquals("desc dev-1", detail.description)
        assertEquals(mapOf(LogLevels.ERROR to 2L, LogLevels.INFO to 2L), detail.levelCounts24h)
        assertEquals(6L, detail.recordsReceived24h)
        assertEquals(3L, detail.recordsDuplicated24h)
    }

    @Test
    fun `WHEN a device never reported anything THEN its page has empty stats instead of failing`() {
        device("dev-2", "Nuevo", account("acc-2", "n@example.com"))

        val detail = dao.deviceDetail("dev-2")!!

        assertNull(detail.summary.online)
        assertNull(detail.summary.lastActivityMs)
        assertEquals(0L, detail.summary.errors24h)
        assertEquals(emptyMap<Int, Long>(), detail.levelCounts24h)
        assertEquals(0L, detail.recordsReceived24h)
    }

    @Test
    fun `WHEN the device or account does not exist THEN null is returned`() {
        assertNull(dao.deviceDetail("ghost"))
        assertNull(dao.accountDetail("ghost"))
    }

    @Test
    fun `WHEN asking for an account THEN it comes with only its own devices`() {
        val a = account("acc-a", "a@example.com")
        val b = account("acc-b", "b@example.com")
        device("dev-a1", "A1", a)
        device("dev-a2", "A2", a)
        device("dev-b1", "B1", b)

        val detail = dao.accountDetail("acc-a")!!

        assertEquals("a@example.com", detail.owner.email)
        assertEquals(listOf("dev-a1", "dev-a2"), detail.devices.map { it.uuid })
    }

    @Test
    fun `WHEN listing devices THEN every device of every owner is there`() {
        device("dev-x", "X", account("acc-x", "x@example.com"))
        device("dev-y", "Y", account("acc-y", "y@example.com"))

        assertEquals(setOf("dev-x", "dev-y"), dao.listDevices().map { it.uuid }.toSet())
    }
}
