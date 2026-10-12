package com.server.croniot.livelog

import com.server.croniot.data.db.daos.AccountJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceEventJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceLogJooqDaoImpl
import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.mqtt.DeviceLogRecord
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Against a real Postgres: the "a resent batch publishes nothing" promise
// rests entirely on INSERT ... ON CONFLICT DO NOTHING RETURNING only
// yielding the rows it really created.
class DeviceLogLiveIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val logDao = DeviceLogJooqDaoImpl(dsl)
    private val eventDao = DeviceEventJooqDaoImpl(dsl)
    private val lookup = DeviceContextLookup(dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun record(seq: Long) = DeviceLogRecord(seq = seq, uptimeMs = seq * 1000, level = 1, tag = "T", message = "m$seq")

    private fun insertDevice(): Long {
        val accountId = AccountJooqDaoImpl(dsl).insert(
            Account(uuid = "acc-uuid", nickname = "nick", email = "owner@example.com", devices = mutableListOf()),
            password = "pwd",
        )
        return DeviceJooqDaoImpl(dsl).insert(
            DeviceEntity(uuid = "dev-uuid", name = "Riego", description = "d", iot = true, accountId = accountId),
        )
    }

    @Test
    fun `WHEN a batch is new THEN insertBatch returns every row with its id`() {
        val deviceId = insertDevice()

        val rows = logDao.insertBatch(deviceId, bootId = 5, records = listOf(record(1), record(2)))

        assertEquals(listOf(1L, 2L), rows.map { it.record.seq })
        assertTrue(rows.all { it.id > 0 })
    }

    @Test
    fun `WHEN the same batch is resent THEN insertBatch returns no rows`() {
        val deviceId = insertDevice()
        logDao.insertBatch(deviceId, bootId = 5, records = listOf(record(1), record(2)))

        val resent = logDao.insertBatch(deviceId, bootId = 5, records = listOf(record(1), record(2)))

        assertEquals(emptyList<Any>(), resent)
    }

    @Test
    fun `WHEN a batch is partly resent THEN only the new rows come back`() {
        val deviceId = insertDevice()
        eventDao.insertBatch(deviceId, bootId = 5, records = listOf(record(1)))

        val rows = eventDao.insertBatch(deviceId, bootId = 5, records = listOf(record(1), record(2)))

        assertEquals(listOf(2L), rows.map { it.record.seq })
    }

    @Test
    fun `WHEN the device exists THEN the lookup returns its name and owner`() {
        val deviceId = insertDevice()

        val context = lookup.byDeviceId(deviceId)

        assertEquals(LogDeviceRef(uuid = "dev-uuid", name = "Riego"), context?.device)
        assertEquals(LogOwnerRef(uuid = "acc-uuid", nickname = "nick", email = "owner@example.com"), context?.owner)
    }

    @Test
    fun `WHEN the device does not exist THEN the lookup returns null`() {
        assertNull(lookup.byDeviceId(999L))
    }
}
