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
                val parsed = result.toBleScanResult() ?: return
                accumulator[parsed.macAddress] = parsed
                trySend(accumulator.values.toList())
            }

            override fun onBatchScanResults(batch: MutableList<ScanResult>) {
                batch.forEach { sr ->
                    val parsed = sr.toBleScanResult() ?: return@forEach
                    accumulator[parsed.macAddress] = parsed
                }
                trySend(accumulator.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            // Iniciamos escaneo sin filtros para ver todos los dispositivos cercanos
            leScanner.startScan(null, settings, callback)
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

    private fun ScanResult.toBleScanResult(): BleScanResult? {
        val record = scanRecord
        val advName = record?.deviceName ?: device.name
        
        // 1. Intentar obtener UUID de los datos de servicio (formato específico Croniot)
        val serviceData = record?.getServiceData(ParcelUuid(BleProfile.SERVICE_UUID))
        val uuidFromService = serviceData?.toString(Charsets.UTF_8)?.takeIf { it.isNotBlank() }
        
        // 2. Intentar obtener UUID del nombre si tiene el prefijo croniot:
        val uuidFromName = advName
            ?.takeIf { it.startsWith(NAME_PREFIX) }
            ?.removePrefix(NAME_PREFIX)
            ?.takeIf { it.isNotBlank() }

        // 3. Verificar si simplemente anuncia el Service UUID de Croniot
        val hasCroniotService = record?.serviceUuids?.contains(ParcelUuid(BleProfile.SERVICE_UUID)) == true

        // Determinamos el ID del dispositivo. 
        // Si no es un dispositivo Croniot reconocido, mostramos su nombre/MAC para verificar que el scanner funciona.
        val deviceUuid = uuidFromService ?: uuidFromName ?: if (hasCroniotService) "croniot-${device.address}" else "dev-${device.address}"

        // Para esta fase de depuración, permitimos mostrar cualquier dispositivo que tenga nombre
        // o que sea explícitamente Croniot. Si no tiene nombre y no es Croniot, lo ignoramos para no llenar la lista de ruido.
        if (advName == null && !hasCroniotService && uuidFromService == null) return null

        return BleScanResult(
            deviceUuid = deviceUuid,
            advertisedName = advName ?: "Dispositivo desconocido",
            macAddress = device.address,
            rssi = rssi,
        )
    }

    private companion object {
        const val NAME_PREFIX = "croniot:"
    }
}
