package com.croniot.client.data.source.local

import androidx.test.core.app.ApplicationProvider
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The underlying `preferencesDataStore` delegate caches its DataStore instance at the JVM level
 * (keyed by file, not by Context instance), so "returns null/default before anything is saved"
 * assertions are order-dependent across test methods within the same test run — a prior test's
 * write can still be visible. Only write-then-read-back assertions are included here since those
 * are deterministic regardless of what ran before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataStoreControllerTest {

    private fun controller() = DataStoreController(ApplicationProvider.getApplicationContext())

    @Test
    fun `WHEN saveCurrentRoute is called THEN getCurrentRoute returns the saved route`() = runTest {
        val c = controller()
        c.saveCurrentRoute("home")

        assertEquals("home", c.getCurrentRoute())
    }

    @Test
    fun `WHEN saveCurrentScreen is called THEN getCurrentScreen returns the saved screen`() = runTest {
        val c = controller()
        c.saveCurrentScreen("DeviceListScreen")

        assertEquals("DeviceListScreen", c.getCurrentScreen())
    }

    @Test
    fun `WHEN generateAndSaveDeviceUuidIfNotExists is called THEN it creates a uuid prefixed with android_`() = runTest {
        val c = controller()
        c.saveCurrentRoute("reset-marker-1")

        c.generateAndSaveDeviceUuidIfNotExists()

        assertTrue(c.getLocalDeviceUuid()!!.startsWith("android_"))
    }

    @Test
    fun `WHEN a uuid already exists THEN generateAndSaveDeviceUuidIfNotExists does not overwrite it`() = runTest {
        val c = controller()
        c.generateAndSaveDeviceUuidIfNotExists()
        val first = c.getLocalDeviceUuid()

        c.generateAndSaveDeviceUuidIfNotExists()

        assertEquals(first, c.getLocalDeviceUuid())
    }

    @Test
    fun `WHEN saveIsForegroundServiceEnabled is called with true THEN reading it back returns true`() = runTest {
        val c = controller()
        c.saveIsForegroundServiceEnabled(true)

        assertEquals(true, c.getIsForegroundServiceEnabled())
    }

    @Test
    fun `WHEN saveIsForegroundServiceEnabled is called with false THEN reading it back returns false`() = runTest {
        val c = controller()
        c.saveIsForegroundServiceEnabled(true)
        c.saveIsForegroundServiceEnabled(false)

        assertEquals(false, c.getIsForegroundServiceEnabled())
    }

    @Test
    fun `WHEN saveServerIp is called THEN getServerIp emits the saved value`() = runTest {
        val c = controller()
        c.saveServerIp("192.168.1.1")

        assertEquals("192.168.1.1", c.getServerIp().first())
    }

    @Test
    fun `WHEN saveCurrentAccount is called THEN getCurrentAccount roundtrips via json`() = runTest {
        val c = controller()
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())

        c.saveCurrentAccount(account)

        assertEquals(account, c.getCurrentAccount())
    }

    @Test
    fun `WHEN saveCurrentAccount is called with null THEN it removes the stored account`() = runTest {
        val c = controller()
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())
        c.saveCurrentAccount(account)

        c.saveCurrentAccount(null)

        assertNull(c.getCurrentAccount())
    }

    @Test
    fun `WHEN saveSelectedDevice is called THEN getSelectedDevice roundtrips via json`() = runTest {
        val c = controller()
        val device = Device(uuid = "device-1", name = "Device", description = "desc")

        c.saveSelectedDevice(device)

        assertEquals(device, c.getSelectedDevice())
    }

    @Test
    fun `WHEN clearAllCacheExceptDeviceUuid removes the selected device THEN getSelectedDevice returns null`() = runTest {
        val c = controller()
        val device = Device(uuid = "device-1", name = "Device", description = "desc")
        c.saveSelectedDevice(device)

        // selected_device is not one of the keys clearAllCacheExceptDeviceUuid preserves,
        // so this deterministically empties it regardless of what other tests ran before.
        c.clearAllCacheExceptDeviceUuid()

        assertNull(c.getSelectedDevice())
    }

    @Test
    fun `WHEN saveAppSessionMode is called THEN getAppSessionMode returns the saved mode`() = runTest {
        val c = controller()
        c.saveAppSessionMode("server")

        assertEquals("server", c.getAppSessionMode())
    }

    @Test
    fun `WHEN saveAppSessionMode is called with null THEN it clears the app session mode`() = runTest {
        val c = controller()
        c.saveAppSessionMode("server")

        c.saveAppSessionMode(null)

        assertNull(c.getAppSessionMode())
    }

    @Test
    fun `WHEN clearAllCacheExceptDeviceUuid is called THEN it preserves device uuid server mode and server ip but clears the rest`() = runTest {
        val c = controller()
        c.generateAndSaveDeviceUuidIfNotExists()
        val deviceUuid = c.getLocalDeviceUuid()
        c.saveServerMode("remote")
        c.saveServerIp("1.2.3.4")
        c.saveCurrentRoute("home")
        c.saveAppSessionMode("server")

        c.clearAllCacheExceptDeviceUuid()

        assertEquals(deviceUuid, c.getLocalDeviceUuid())
        assertEquals("remote", c.getCurrentServerMode().first())
        assertEquals("1.2.3.4", c.getServerIp().first())
        assertNull(c.getCurrentRoute())
        assertNull(c.getAppSessionMode())
    }

    @Test
    fun `WHEN saveServerMode is called THEN getServerMode emits the saved mode`() = runTest {
        val c = controller()
        c.saveServerMode("local")

        assertEquals("local", c.getServerMode().first())
    }

    @Test
    fun `WHEN saveEmail is called THEN it does not throw`() = runTest {
        // No public getter exposes account_email; this only protects the write path from regressing.
        controller().saveEmail("user@example.com")
    }

    @Test
    fun `WHEN getLocalDeviceToken is called THEN it does not throw`() = runTest {
        // No public setter exists on this class for device_token; this only protects the read path.
        controller().getLocalDeviceToken()
    }
}
