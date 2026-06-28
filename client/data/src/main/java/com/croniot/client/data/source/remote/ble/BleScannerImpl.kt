package com.croniot.client.data.source.remote.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@SuppressLint("MissingPermission")
class BleScannerImpl(
    private val context: Context,
    private val permissionsHelper: BlePermissionsHelper,
) : BleScanner {

    override fun scan(): Flow<List<BleScanResult>> = callbackFlow {
        if (!permissionsHelper.allGranted()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter
        val leScanner = adapter?.bluetoothLeScanner
        if (leScanner == null || adapter.isEnabled != true) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val accumulator = mutableMapOf<String, BleScanResult>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val parsed = result.toBleScanResult()
                accumulator[parsed.macAddress] = parsed
                trySend(accumulator.values.toList())
            }

            override fun onBatchScanResults(batch: MutableList<ScanResult>) {
                batch.forEach { sr ->
                    val parsed = sr.toBleScanResult()
                    accumulator[parsed.macAddress] = parsed
                }
                trySend(accumulator.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed: $errorCode"))
            }
        }

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleProfile.SERVICE_UUID))
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            leScanner.startScan(filters, settings, callback)
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }
        trySend(emptyList())

        awaitClose {
            try {
                leScanner.stopScan(callback)
            } catch (_: Exception) {
                // best-effort
            }
        }
    }

    private fun ScanResult.toBleScanResult(): BleScanResult {
        val advName = scanRecord?.deviceName ?: device.name
        return BleScanResult(
            macAddress = device.address,
            advertisedName = advName,
            rssi = rssi,
        )
    }
}
