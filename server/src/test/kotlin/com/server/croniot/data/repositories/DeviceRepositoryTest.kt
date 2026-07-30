package com.server.croniot.data.repositories

import com.server.croniot.testsupport.fakes.FakeDeviceDao
import croniot.models.Device
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DeviceRepository] using a fake [com.server.croniot.data.db.daos.DeviceDao] (not
 * a mock): most methods are plain delegation, but [DeviceRepository.createDevice] maps a domain
 * [Device] into a [com.server.croniot.data.db.entities.DeviceEntity] before persisting it, which
 * is real logic worth exercising with realistic write-then-read behavior.
 */
class DeviceRepositoryTest {

    private val deviceDao = FakeDeviceDao()
    private val repository = DeviceRepository(deviceDao)

    @Test
    fun `getId returns the id for a known uuid and null otherwise`() {
        val id = deviceDao.seed(deviceEntity(uuid = "device-uuid"))

        assertEquals(id, repository.getId("device-uuid"))
        assertNull(repository.getId("missing-uuid"))
    }

    @Test
    fun `isDeviceExists is true for a known uuid and false otherwise`() {
        deviceDao.seed(deviceEntity(uuid = "device-uuid"))

        assertTrue(repository.isDeviceExists("device-uuid"))
        assertFalse(repository.isDeviceExists("missing-uuid"))
    }

    @Test
    fun `getByUuid returns the device for a known uuid and null otherwise`() {
        deviceDao.seed(deviceEntity(uuid = "device-uuid", name = "Kitchen sensor"))

        assertEquals("Kitchen sensor", repository.getByUuid("device-uuid")?.name)
        assertNull(repository.getByUuid("missing-uuid"))
    }

    @Test
    fun `getLazy returns the device for a known uuid and null otherwise`() {
        deviceDao.seed(deviceEntity(uuid = "device-uuid", name = "Kitchen sensor"))

        assertEquals("Kitchen sensor", repository.getLazy("device-uuid")?.name)
        assertNull(repository.getLazy("missing-uuid"))
    }

    @Test
    fun `getAll returns every seeded device`() {
        deviceDao.seed(deviceEntity(uuid = "device-1"))
        deviceDao.seed(deviceEntity(uuid = "device-2"))

        assertEquals(setOf("device-1", "device-2"), repository.getAll().map { it.uuid }.toSet())
    }

    @Test
    fun `createDevice maps the domain device into an entity and persists it so it becomes retrievable`() {
        val device = Device(uuid = "new-device", name = "New device", description = "desc", iot = true)

        val id = repository.createDevice(device, accountId = 7L)

        assertTrue(id > 0)
        val stored = repository.getByUuid("new-device")
        assertEquals("New device", stored?.name)
        assertEquals("desc", stored?.description)
        assertTrue(stored?.iot == true)
    }

    private fun deviceEntity(
        uuid: String,
        name: String = "Device",
        description: String = "",
        iot: Boolean = true,
        accountId: Long = 1L,
    ) = com.server.croniot.data.db.entities.DeviceEntity(
        uuid = uuid,
        name = name,
        description = description,
        iot = iot,
        accountId = accountId,
    )
}
