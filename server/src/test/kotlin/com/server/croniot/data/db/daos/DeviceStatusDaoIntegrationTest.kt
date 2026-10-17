package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.testsupport.PostgresTestcontainer
import croniot.models.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeviceStatusDaoIntegrationTest {

    private val dsl = PostgresTestcontainer.dsl
    private val dao = DeviceStatusDao(dsl)

    @BeforeEach
    fun setUp() {
        PostgresTestcontainer.reset()
    }

    private fun insertDevice(): Long {
        val accountId = AccountJooqDaoImpl(dsl).insert(
            Account(uuid = "acc", nickname = "n", email = "a@example.com", devices = mutableListOf()),
            password = "pwd",
        )
        return DeviceJooqDaoImpl(
            dsl
        ).insert(DeviceEntity(uuid = "dev", name = "n", description = "d", iot = true, accountId = accountId))
    }

    @Test
    fun `WHEN a device has never reported THEN it has no status`() {
        assertNull(dao.get(insertDevice()))
    }

    @Test
    fun `WHEN a device goes online then offline THEN the row is overwritten and the boot is kept`() {
        val deviceId = insertDevice()

        dao.upsert(deviceId, online = true, bootId = 96)
        val online = dao.get(deviceId)!!
        dao.upsert(deviceId, online = false, bootId = null)
        val offline = dao.get(deviceId)!!

        assertTrue(online.online)
        assertEquals(96L, online.bootId)
        assertFalse(offline.online)
        assertEquals(96L, offline.bootId)
    }

    @Test
    fun `WHEN the device reboots THEN the new boot replaces the old one`() {
        val deviceId = insertDevice()

        dao.upsert(deviceId, online = true, bootId = 96)
        dao.upsert(deviceId, online = true, bootId = 97)

        assertEquals(97L, dao.get(deviceId)!!.bootId)
    }
}
