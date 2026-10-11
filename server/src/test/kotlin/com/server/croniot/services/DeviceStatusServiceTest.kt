package com.server.croniot.services

import com.server.croniot.data.db.daos.DeviceStatusDao
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.services.DeviceStatusService.Status
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DeviceStatusServiceTest {

    private val deviceRepository: DeviceRepository = mockk()
    private val dao: DeviceStatusDao = mockk(relaxUnitFun = true)
    private val service = DeviceStatusService(deviceRepository, dao)

    @Test
    fun `WHEN parsing status payloads THEN LWT, birth and garbage are told apart`() {
        assertEquals(Status.Offline, DeviceStatusService.parse("offline"))
        assertEquals(Status.Online(96), DeviceStatusService.parse("{\"boot\":96}"))
        assertEquals(Status.Online(96), DeviceStatusService.parse("{\"boot\":96}\u0000"))
        assertEquals(Status.Online(null), DeviceStatusService.parse("{}"))
        assertNull(DeviceStatusService.parse("hello"))
    }

    @Test
    fun `WHEN a birth arrives THEN the device is stored as online with its boot`() {
        every { deviceRepository.getId("dev-1") } returns 7L

        service.onStatusMessage("dev-1", "{\"boot\":96}")

        verify { dao.upsert(7L, online = true, bootId = 96L) }
    }

    @Test
    fun `WHEN the LWT arrives THEN the device is stored as offline keeping its boot`() {
        every { deviceRepository.getId("dev-1") } returns 7L

        service.onStatusMessage("dev-1", "offline")

        verify { dao.upsert(7L, online = false, bootId = null) }
    }

    @Test
    fun `WHEN the device is unknown or the payload unrecognised THEN nothing is stored`() {
        every { deviceRepository.getId("ghost") } returns null

        service.onStatusMessage("ghost", "offline")
        service.onStatusMessage("dev-1", "garbage")

        verify(exactly = 0) { dao.upsert(any(), any(), any()) }
    }
}
