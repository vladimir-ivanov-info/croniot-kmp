package com.croniot.client.data.source.remote.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

interface BlePermissionsHelper {
    fun requiredPermissions(): List<String>
    fun missingPermissions(): List<String>
    fun allGranted(): Boolean
}

class BlePermissionsHelperImpl(
    private val context: Context,
) : BlePermissionsHelper {

    override fun requiredPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun missingPermissions(): List<String> =
        requiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

    override fun allGranted(): Boolean = missingPermissions().isEmpty()
}
