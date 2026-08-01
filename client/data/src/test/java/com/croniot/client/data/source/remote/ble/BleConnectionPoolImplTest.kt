package com.croniot.client.data.source.remote.ble

import Outcome
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.croniot.client.domain.errors.BleError
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BleConnectionPoolImplTest {

    private val context: Context = mockk(relaxed = true)
    private val device: BluetoothDevice = mockk()

    private fun buildPool(
        maxConnections: Int = 4,
        factory: (String, BluetoothDevice) -> BleConnection,
    ) = BleConnectionPoolImpl(context = context, maxConnections = maxConnections, connectionFactory = factory)

    private fun connectionMock(uuid: String, connectOutcome: Outcome<Unit, BleError> = Outcome.Ok(Unit)): BleConnection {
        val connection: BleConnection = mockk()
        coEvery { connection.connectAndAuthenticate(any(), any()) } returns connectOutcome
        coJustRun { connection.close() }
        return connection
    }

    @Test
    fun `WHEN connection succeeds THEN getOrConnect creates and caches a new connection`() = runTest {
        val connection = connectionMock("device-1")
        val pool = buildPool { _, _ -> connection }

        val result = pool.getOrConnect("device-1", device, "user", "pass")

        assertEquals(Outcome.Ok(connection), result)
        assertSame(connection, pool.get("device-1"))
    }

    @Test
    fun `WHEN a connection is already cached THEN getOrConnect returns it without invoking the factory again`() = runTest {
        var factoryCalls = 0
        val connection = connectionMock("device-1")
        val pool = buildPool { _, _ -> factoryCalls++; connection }

        pool.getOrConnect("device-1", device, "user", "pass")
        pool.getOrConnect("device-1", device, "user", "pass")

        assertEquals(1, factoryCalls)
    }

    @Test
    fun `WHEN connection fails THEN getOrConnect closes the connection and does not cache it`() = runTest {
        val connection = connectionMock("device-1", connectOutcome = Outcome.Err(BleError.Timeout))
        val pool = buildPool { _, _ -> connection }

        val result = pool.getOrConnect("device-1", device, "user", "pass")

        assertEquals(Outcome.Err(BleError.Timeout), result)
        assertNull(pool.get("device-1"))
        coVerify(exactly = 1) { connection.close() }
    }

    @Test
    fun `WHEN a device was never connected THEN get returns null`() {
        val pool = buildPool { _, _ -> connectionMock("unused") }

        assertNull(pool.get("device-1"))
    }

    @Test
    fun `WHEN close is called THEN it removes and closes the specific connection`() = runTest {
        val connection = connectionMock("device-1")
        val pool = buildPool { _, _ -> connection }
        pool.getOrConnect("device-1", device, "user", "pass")

        pool.close("device-1")

        assertNull(pool.get("device-1"))
        coVerify(exactly = 1) { connection.close() }
    }

    @Test
    fun `WHEN closeAll is called THEN it closes and clears every connection`() = runTest {
        val connectionA = connectionMock("device-a")
        val connectionB = connectionMock("device-b")
        val pool = buildPool { uuid, _ -> if (uuid == "device-a") connectionA else connectionB }
        pool.getOrConnect("device-a", device, "user", "pass")
        pool.getOrConnect("device-b", device, "user", "pass")

        pool.closeAll()

        assertNull(pool.get("device-a"))
        assertNull(pool.get("device-b"))
        coVerify(exactly = 1) { connectionA.close() }
        coVerify(exactly = 1) { connectionB.close() }
    }

    @Test
    fun `WHEN max connections is reached THEN getOrConnect evicts the oldest connection`() = runTest {
        val connectionA = connectionMock("device-a")
        val connectionB = connectionMock("device-b")
        val connectionC = connectionMock("device-c")
        val pool = buildPool(maxConnections = 2) { uuid, _ ->
            when (uuid) {
                "device-a" -> connectionA
                "device-b" -> connectionB
                else -> connectionC
            }
        }

        pool.getOrConnect("device-a", device, "user", "pass")
        pool.getOrConnect("device-b", device, "user", "pass")
        pool.getOrConnect("device-c", device, "user", "pass")

        assertTrue(pool.get("device-a") == null || pool.get("device-b") == null)
        assertSame(connectionC, pool.get("device-c"))
    }
}
