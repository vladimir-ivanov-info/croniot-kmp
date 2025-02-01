package com.croniot.android.core.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object DevicePropertiesController {

    fun getScreenDetails(context: Context): Map<String, String> {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        return mapOf(
            "widthPixels" to displayMetrics.widthPixels.toString(),
            "heightPixels" to displayMetrics.heightPixels.toString(),
            "densityDpi" to displayMetrics.densityDpi.toString(),
            "density" to displayMetrics.density.toString(),
            "scaledDensity" to displayMetrics.scaledDensity.toString(),
            "xdpi" to displayMetrics.xdpi.toString(),
            "ydpi" to displayMetrics.ydpi.toString(),
            // "displayRefreshRate" to getRefreshRate(context)
        )
    }

    fun getRefreshRate(context: Context): Float {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = context.display ?: windowManager.defaultDisplay
            display.refreshRate
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
    }

    fun getDeviceDetails(): Map<String, String> {
        return mapOf(
            "Android Version" to Build.VERSION.RELEASE.orEmpty(),
            "SDK Version" to Build.VERSION.SDK_INT.toString(),
            "Manufacturer" to Build.MANUFACTURER.orEmpty(),
            "Brand" to Build.BRAND.orEmpty(),
            "Device Model" to Build.MODEL.orEmpty(),
            "Product Name" to Build.PRODUCT.orEmpty(),
            "Device Codename" to Build.DEVICE.orEmpty(),
            "Hardware" to Build.HARDWARE.orEmpty(),
            /*"Board" to Build.BOARD.orEmpty(),
            "Fingerprint" to Build.FINGERPRINT.orEmpty(),
            "Build ID" to Build.ID.orEmpty(),
            "Build Tags" to Build.TAGS.orEmpty(),
            "Build Type" to Build.TYPE.orEmpty(),
            "Build Host" to Build.HOST.orEmpty(),
            "Build User" to Build.USER.orEmpty(),
            "Incremental Version" to Build.VERSION.INCREMENTAL.orEmpty(),
            "Codename" to Build.VERSION.CODENAME.orEmpty(),
            "Base OS" to Build.VERSION.BASE_OS.orEmpty(),
            "Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", ")*/
        )
    }
}
