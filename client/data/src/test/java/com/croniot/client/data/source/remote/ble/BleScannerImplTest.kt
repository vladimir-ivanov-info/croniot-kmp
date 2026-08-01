package com.croniot.client.data.source.remote.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BleScannerImplTest {

    private val context: Context = mockk()
    private val permissionsHelper: BlePermissionsHelper = mockk()

    private fun scanner() = BleScannerImpl(context, permissionsHelper)

    @AfterEach
    fun tearDown() {
        unmockkConstructor(ParcelUuid::class, ScanFilter.Builder::class, ScanSettings.Builder::class)
    }

    // Real android.jar in unit tests throws for these builders' method bodies ("not mocked"),
    // so their construction must be intercepted for the happy path to reach leScanner.startScan().
    private fun stubScanFilterAndSettingsConstruction() {
        mockkConstructor(ParcelUuid::class, ScanFilter.Builder::class, ScanSettings.Builder::class)
        every { anyConstructed<ScanFilter.Builder>().setServiceUuid(any()) } returns mockk(relaxed = true)
        every { anyConstructed<ScanSettings.Builder>().setScanMode(any()) } returns mockk(relaxed = true)
    }

    private fun mockAdapterWithScanner(leScanner: BluetoothLeScanner, enabled: Boolean = true): BluetoothAdapter {
        val adapter: BluetoothAdapter = mockk {
            every { bluetoothLeScanner } returns leScanner
            every { isEnabled } returns enabled
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager
        return adapter
    }

    private fun mockScanResult(mac: String, name: String?, rssiValue: Int): ScanResult {
        val device: BluetoothDevice = mockk {
            every { address } returns mac
            every { this@mockk.name } returns name
        }
        return mockk {
            every { this@mockk.device } returns device
            every { scanRecord } returns null
            every { rssi } returns rssiValue
        }
    }

    @Test
    fun `WHEN permissions are not granted THEN scan emits empty list immediately`() = runTest {
        every { permissionsHelper.allGranted() } returns false

        val result = scanner().scan().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN bluetooth service is unavailable THEN scan emits empty list`() = runTest {
        every { permissionsHelper.allGranted() } returns true
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns null

        val result = scanner().scan().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN bluetooth adapter is disabled THEN scan emits empty list`() = runTest {
        every { permissionsHelper.allGranted() } returns true
        val leScanner: android.bluetooth.le.BluetoothLeScanner = mockk()
        val adapter: BluetoothAdapter = mockk {
            every { bluetoothLeScanner } returns leScanner
            every { isEnabled } returns false
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        val result = scanner().scan().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN bluetoothLeScanner is null THEN scan emits empty list`() = runTest {
        every { permissionsHelper.allGranted() } returns true
        val adapter: BluetoothAdapter = mockk {
            every { bluetoothLeScanner } returns null
            every { isEnabled } returns true
        }
        val manager: BluetoothManager = mockk { every { this@mockk.adapter } returns adapter }
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        val result = scanner().scan().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `WHEN scan is started THEN it starts the BLE scan and accumulates results from the callback`() = runTest {
        stubScanFilterAndSettingsConstruction()
        every { permissionsHelper.allGranted() } returns true
        val leScanner: BluetoothLeScanner = mockk()
        mockAdapterWithScanner(leScanner)

        val callbackSlot = slot<ScanCallback>()
        every {
            leScanner.startScan(any<List<ScanFilter>>(), any<ScanSettings>(), capture(callbackSlot))
        } just Runs
        every { leScanner.stopScan(any<ScanCallback>()) } just Runs

        scanner().scan().test {
            assertEquals(emptyList<BleScanResult>(), awaitItem())

            callbackSlot.captured.onScanResult(0, mockScanResult("AA:BB:CC:DD:EE:01", "Device1", -40))
            val afterFirstResult = awaitItem()
            assertEquals(1, afterFirstResult.size)
            assertEquals(
                BleScanResult(macAddress = "AA:BB:CC:DD:EE:01", advertisedName = "Device1", rssi = -40),
                afterFirstResult[0],
            )

            callbackSlot.captured.onBatchScanResults(
                mutableListOf(
                    mockScanResult("AA:BB:CC:DD:EE:01", "Device1", -35),
                    mockScanResult("AA:BB:CC:DD:EE:02", "Device2", -60),
                )
            )
            val afterBatch = awaitItem()
            assertEquals(2, afterBatch.size)
            assertTrue(afterBatch.any { it.macAddress == "AA:BB:CC:DD:EE:02" && it.advertisedName == "Device2" })

            cancelAndIgnoreRemainingEvents()
        }

        verify { leScanner.stopScan(any<ScanCallback>()) }
    }

    @Test
    fun `WHEN onScanFailed is invoked THEN scan closes the flow with an error`() = runTest {
        stubScanFilterAndSettingsConstruction()
        every { permissionsHelper.allGranted() } returns true
        val leScanner: BluetoothLeScanner = mockk()
        mockAdapterWithScanner(leScanner)

        val callbackSlot = slot<ScanCallback>()
        every {
            leScanner.startScan(any<List<ScanFilter>>(), any<ScanSettings>(), capture(callbackSlot))
        } just Runs
        every { leScanner.stopScan(any<ScanCallback>()) } just Runs

        var thrown: Throwable? = null
        scanner().scan().test {
            awaitItem() // initial empty list
            callbackSlot.captured.onScanFailed(1)
            thrown = awaitError()
        }

        assertTrue(thrown is IllegalStateException)
        assertTrue(thrown?.message?.contains("1") == true)
    }

    @Test
    fun `WHEN startScan throws SecurityException THEN scan closes the flow with an error`() = runTest {
        stubScanFilterAndSettingsConstruction()
        every { permissionsHelper.allGranted() } returns true
        val leScanner: BluetoothLeScanner = mockk()
        mockAdapterWithScanner(leScanner)

        every {
            leScanner.startScan(any<List<ScanFilter>>(), any<ScanSettings>(), any<ScanCallback>())
        } throws SecurityException("no permission")

        var thrown: Throwable? = null
        scanner().scan().test {
            thrown = awaitError()
        }

        assertTrue(thrown is SecurityException)
    }
}
