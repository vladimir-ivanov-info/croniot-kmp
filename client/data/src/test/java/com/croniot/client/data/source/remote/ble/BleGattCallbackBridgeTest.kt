package com.croniot.client.data.source.remote.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class BleGattCallbackBridgeTest {

    private lateinit var bridge: BleGattCallbackBridge
    private val gatt: BluetoothGatt = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        bridge = BleGattCallbackBridge()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `WHEN onConnectionStateChange is called THEN it updates connectionState`() {
        bridge.onConnectionStateChange(gatt, 0, BluetoothProfile.STATE_CONNECTED)

        assertEquals(BluetoothProfile.STATE_CONNECTED, bridge.connectionState.value)
    }

    @Test
    fun `WHEN onServicesDiscovered is called THEN it sends status through channel`() = runTest {
        bridge.onServicesDiscovered(gatt, BluetoothGatt.GATT_SUCCESS)

        assertEquals(BluetoothGatt.GATT_SUCCESS, bridge.servicesDiscovered.receive())
    }

    @Test
    fun `WHEN onMtuChanged is called THEN it sends MtuAck through channel`() = runTest {
        bridge.onMtuChanged(gatt, 247, BluetoothGatt.GATT_SUCCESS)

        val ack = bridge.mtuChanged.receive()
        assertEquals(247, ack.mtu)
        assertEquals(BluetoothGatt.GATT_SUCCESS, ack.status)
    }

    @Test
    fun `WHEN onDescriptorWrite is called THEN it sends DescriptorAck with characteristic and descriptor uuids`() = runTest {
        val charUuid = UUID.randomUUID()
        val descUuid = UUID.randomUUID()
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns charUuid }
        val descriptor: BluetoothGattDescriptor = mockk {
            every { uuid } returns descUuid
            every { this@mockk.characteristic } returns characteristic
        }

        bridge.onDescriptorWrite(gatt, descriptor, BluetoothGatt.GATT_SUCCESS)

        val ack = bridge.descriptorWrites.receive()
        assertEquals(charUuid, ack.characteristicUuid)
        assertEquals(descUuid, ack.descriptorUuid)
    }

    @Test
    fun `WHEN onCharacteristicRead is called THEN it sends ReadAck with value bytes`() = runTest {
        val charUuid = UUID.randomUUID()
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns charUuid }
        val bytes = byteArrayOf(1, 2, 3)

        bridge.onCharacteristicRead(gatt, characteristic, bytes, BluetoothGatt.GATT_SUCCESS)

        val ack = bridge.characteristicReads.receive()
        assertEquals(charUuid, ack.characteristicUuid)
        assertEquals(3, ack.value.size)
    }

    @Test
    fun `WHEN onCharacteristicWrite is called THEN it sends WriteAck`() = runTest {
        val charUuid = UUID.randomUUID()
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns charUuid }

        bridge.onCharacteristicWrite(gatt, characteristic, BluetoothGatt.GATT_SUCCESS)

        val ack = bridge.characteristicWrites.receive()
        assertEquals(charUuid, ack.characteristicUuid)
        assertEquals(BluetoothGatt.GATT_SUCCESS, ack.status)
    }

    @Test
    fun `WHEN onCharacteristicChanged is called with the auth uuid THEN it sends the decoded payload to authNotification channel`() = runTest {
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns BleProfile.CHARACTERISTIC_AUTH }

        bridge.onCharacteristicChanged(gatt, characteristic, "token123".toByteArray())

        assertEquals("token123", bridge.authNotification.receive())
    }

    @Test
    fun `WHEN onCharacteristicChanged is called with the sync data uuid and enough bytes THEN it parses seq total and data`() = runTest {
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns BleProfile.CHARACTERISTIC_SYNC_DATA }
        val value = byteArrayOf(2, 5, 10, 20, 30)

        bridge.onCharacteristicChanged(gatt, characteristic, value)

        val chunk = bridge.syncDataChunks.receive()
        assertEquals(2, chunk.seq)
        assertEquals(5, chunk.total)
        assertEquals(listOf<Byte>(10, 20, 30), chunk.data.toList())
    }

    @Test
    fun `WHEN onCharacteristicChanged is called with the sync data uuid but too few bytes THEN it is ignored`() = runTest {
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns BleProfile.CHARACTERISTIC_SYNC_DATA }

        bridge.onCharacteristicChanged(gatt, characteristic, byteArrayOf(1))

        assertTrue(bridge.syncDataChunks.tryReceive().isFailure)
    }

    @Test
    fun `WHEN onCharacteristicChanged is called with another uuid THEN it emits a NotificationEvent`() = runTest {
        val otherUuid = UUID.randomUUID()
        val characteristic: BluetoothGattCharacteristic = mockk { every { uuid } returns otherUuid }

        val collected = mutableListOf<BleGattCallbackBridge.NotificationEvent>()
        val job = kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            bridge.notifications.collect { collected.add(it) }
        }

        bridge.onCharacteristicChanged(gatt, characteristic, "hello".toByteArray())

        assertEquals(1, collected.size)
        assertEquals("hello", collected.first().payload)
        assertEquals(otherUuid, collected.first().characteristicUuid)
        job.cancel()
    }

    @Test
    fun `WHEN onReadRemoteRssi is called with success status THEN it sends rssi through channel`() = runTest {
        bridge.onReadRemoteRssi(gatt, -60, BluetoothGatt.GATT_SUCCESS)

        assertEquals(-60, bridge.rssiReads.receive())
    }

    @Test
    fun `WHEN onReadRemoteRssi is called with failure status THEN it does not send anything`() = runTest {
        bridge.onReadRemoteRssi(gatt, -60, BluetoothGatt.GATT_FAILURE)

        assertTrue(bridge.rssiReads.tryReceive().isFailure)
    }

    @Test
    fun `WHEN close is called THEN it closes all channels`() = runTest {
        bridge.close()

        assertTrue(bridge.servicesDiscovered.isClosedForSend)
        assertTrue(bridge.mtuChanged.isClosedForSend)
        assertTrue(bridge.characteristicReads.isClosedForSend)
    }
}
