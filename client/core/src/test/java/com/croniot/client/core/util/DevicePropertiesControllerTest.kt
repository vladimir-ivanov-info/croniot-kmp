package com.croniot.client.core.util

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DevicePropertiesControllerTest {

    @Test
    fun `getScreenDetails maps DisplayMetrics fields to strings`() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
            densityDpi = 420
            density = 2.625f
            scaledDensity = 2.625f
            xdpi = 411.8f
            ydpi = 411.8f
        }
        val resources: Resources = mockk { every { this@mockk.displayMetrics } returns displayMetrics }
        val context: Context = mockk { every { this@mockk.resources } returns resources }

        val details = DevicePropertiesController.getScreenDetails(context)

        assertEquals("1080", details["widthPixels"])
        assertEquals("1920", details["heightPixels"])
        assertEquals("420", details["densityDpi"])
    }

    @Test
    fun `getRefreshRate on legacy SDK reads defaultDisplay refreshRate`() {
        val display: android.view.Display = mockk { every { refreshRate } returns 60f }
        val windowManager: WindowManager = mockk { every { defaultDisplay } returns display }
        val context: Context = mockk {
            every { getSystemService(Context.WINDOW_SERVICE) } returns windowManager
        }

        val refreshRate = DevicePropertiesController.getRefreshRate(context)

        assertEquals(60f, refreshRate)
    }

    @Test
    fun `getDeviceDetails returns a map with all expected keys`() {
        val details = DevicePropertiesController.getDeviceDetails()

        assertTrue(details.containsKey("Android Version"))
        assertTrue(details.containsKey("SDK Version"))
        assertTrue(details.containsKey("Manufacturer"))
        assertTrue(details.containsKey("Brand"))
        assertTrue(details.containsKey("Device Model"))
        assertTrue(details.containsKey("Product Name"))
        assertTrue(details.containsKey("Device Codename"))
        assertTrue(details.containsKey("Hardware"))
    }

    @Test
    fun `getDeviceDetails returns exactly eight entries`() {
        val details = DevicePropertiesController.getDeviceDetails()

        assertTrue(details.size == 8)
    }

    @Test
    fun `getDeviceDetails does not throw and values are non-null strings`() {
        val details = DevicePropertiesController.getDeviceDetails()

        details.values.forEach { value ->
            assertTrue(value is String)
        }
    }
}
