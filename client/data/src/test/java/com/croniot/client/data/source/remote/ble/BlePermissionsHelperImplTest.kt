package com.croniot.client.data.source.remote.ble

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [30])
class BlePermissionsHelperImplTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun helper() = BlePermissionsHelperImpl(context)

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `WHEN SDK is below S THEN requiredPermissions requests legacy bluetooth and location permissions`() {
        val required = helper().requiredPermissions()

        assertEquals(
            listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION),
            required,
        )
    }

    @Test
    fun `WHEN none of the permissions are granted THEN missingPermissions returns all required permissions`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED

        val missing = helper().missingPermissions()

        assertEquals(helper().requiredPermissions(), missing)
    }

    @Test
    fun `WHEN permissions are missing THEN allGranted is false`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED

        assertFalse(helper().allGranted())
    }

    @Test
    fun `WHEN every required permission is granted THEN allGranted is true`() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED

        assertTrue(helper().allGranted())
    }

    @Test
    fun `WHEN some permissions were granted THEN missingPermissions excludes them`() {
        val required = helper().requiredPermissions()
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(context, any()) } returns PackageManager.PERMISSION_DENIED
        every { ContextCompat.checkSelfPermission(context, required.first()) } returns PackageManager.PERMISSION_GRANTED

        val missing = helper().missingPermissions()

        assertFalse(missing.contains(required.first()))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [31])
class BlePermissionsHelperImplSApiTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `WHEN SDK is S and above THEN requiredPermissions requests scan and connect permissions`() {
        val required = BlePermissionsHelperImpl(context).requiredPermissions()

        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
            required,
        )
    }
}
