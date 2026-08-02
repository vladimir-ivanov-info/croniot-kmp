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
    fun `WHEN a device is unknown THEN transportFor defaults to CLOUD`() {
        assertEquals(TransportKind.CLOUD, router.transportFor("device-1"))
    }

    @Test
    fun `WHEN markBle is called THEN transportFor returns BLE`() = runTest {
        router.markBle("device-1")

        assertEquals(TransportKind.BLE, router.transportFor("device-1"))
    }

    @Test
    fun `WHEN markCloud is called after markBle THEN transportFor reverts to CLOUD`() = runTest {
        router.markBle("device-1")

        router.markCloud("device-1")

        assertEquals(TransportKind.CLOUD, router.transportFor("device-1"))
    }

    @Test
    fun `WHEN markBle is called for one deviceUuid THEN it only affects that device`() = runTest {
        router.markBle("device-1")

        assertEquals(TransportKind.CLOUD, router.transportFor("device-2"))
    }

    @Test
    fun `WHEN devices are marked ble THEN bleDeviceUuids reflects them`() = runTest {
        router.markBle("device-1")
        router.markBle("device-2")

        assertEquals(setOf("device-1", "device-2"), router.bleDeviceUuids.value)
    }

    @Test
    fun `WHEN loadInitial is called THEN it populates bleDeviceUuids from the dao`() = runTest {
        coEvery { bleKnownDeviceDao.getAllUuids() } returns listOf("device-a", "device-b")

        router.loadInitial()

        assertEquals(setOf("device-a", "device-b"), router.bleDeviceUuids.value)
        assertEquals(TransportKind.BLE, router.transportFor("device-a"))
    }

    @Test
    fun `WHEN the dao returns an empty result THEN loadInitial clears bleDeviceUuids`() = runTest {
        router.markBle("device-1")
        coEvery { bleKnownDeviceDao.getAllUuids() } returns emptyList()

        router.loadInitial()

        assertEquals(emptySet<String>(), router.bleDeviceUuids.value)
    }

    @Test
    fun `WHEN markCloud is called on a device never marked ble THEN it is a no-op`() = runTest {
        router.markCloud("unknown-device")

        assertEquals(emptySet<String>(), router.bleDeviceUuids.value)
    }
}
