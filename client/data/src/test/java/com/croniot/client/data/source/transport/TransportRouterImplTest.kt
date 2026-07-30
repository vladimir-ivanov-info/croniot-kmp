package com.croniot.client.data.source.transport

import com.croniot.client.data.source.local.database.daos.BleKnownDeviceDao
import com.croniot.client.domain.models.TransportKind
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransportRouterImplTest {

    private val bleKnownDeviceDao: BleKnownDeviceDao = mockk()
    private val router = TransportRouterImpl(bleKnownDeviceDao)

    @Test
    fun `transportFor an unknown device defaults to CLOUD`() {
        assertEquals(TransportKind.CLOUD, router.transportFor("device-1"))
    }

    @Test
    fun `markBle then transportFor returns BLE`() = runTest {
        router.markBle("device-1")

        assertEquals(TransportKind.BLE, router.transportFor("device-1"))
    }

    @Test
    fun `markCloud after markBle reverts transportFor to CLOUD`() = runTest {
        router.markBle("device-1")

        router.markCloud("device-1")

        assertEquals(TransportKind.CLOUD, router.transportFor("device-1"))
    }

    @Test
    fun `markBle only affects the given deviceUuid`() = runTest {
        router.markBle("device-1")

        assertEquals(TransportKind.CLOUD, router.transportFor("device-2"))
    }

    @Test
    fun `bleDeviceUuids reflects marked devices`() = runTest {
        router.markBle("device-1")
        router.markBle("device-2")

        assertEquals(setOf("device-1", "device-2"), router.bleDeviceUuids.value)
    }

    @Test
    fun `loadInitial populates bleDeviceUuids from the dao`() = runTest {
        coEvery { bleKnownDeviceDao.getAllUuids() } returns listOf("device-a", "device-b")

        router.loadInitial()

        assertEquals(setOf("device-a", "device-b"), router.bleDeviceUuids.value)
        assertEquals(TransportKind.BLE, router.transportFor("device-a"))
    }

    @Test
    fun `loadInitial with empty dao result clears bleDeviceUuids`() = runTest {
        router.markBle("device-1")
        coEvery { bleKnownDeviceDao.getAllUuids() } returns emptyList()

        router.loadInitial()

        assertEquals(emptySet<String>(), router.bleDeviceUuids.value)
    }

    @Test
    fun `markCloud on a device never marked ble is a no-op`() = runTest {
        router.markCloud("unknown-device")

        assertEquals(emptySet<String>(), router.bleDeviceUuids.value)
    }
}
