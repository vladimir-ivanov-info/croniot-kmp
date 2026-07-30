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
    fun `saveCurrentRoute then getCurrentRoute returns the saved route`() = runTest {
        val c = controller()
        c.saveCurrentRoute("home")

        assertEquals("home", c.getCurrentRoute())
    }

    @Test
    fun `saveCurrentScreen then getCurrentScreen returns the saved screen`() = runTest {
        val c = controller()
        c.saveCurrentScreen("DeviceListScreen")

        assertEquals("DeviceListScreen", c.getCurrentScreen())
    }

    @Test
    fun `generateAndSaveDeviceUuidIfNotExists creates a uuid prefixed with android_`() = runTest {
        val c = controller()
        c.saveCurrentRoute("reset-marker-1")

        c.generateAndSaveDeviceUuidIfNotExists()

        assertTrue(c.getLocalDeviceUuid()!!.startsWith("android_"))
    }

    @Test
    fun `generateAndSaveDeviceUuidIfNotExists does not overwrite an existing uuid`() = runTest {
        val c = controller()
        c.generateAndSaveDeviceUuidIfNotExists()
        val first = c.getLocalDeviceUuid()

        c.generateAndSaveDeviceUuidIfNotExists()

        assertEquals(first, c.getLocalDeviceUuid())
    }

    @Test
    fun `saveIsForegroundServiceEnabled true then read back returns true`() = runTest {
        val c = controller()
        c.saveIsForegroundServiceEnabled(true)

        assertEquals(true, c.getIsForegroundServiceEnabled())
    }

    @Test
    fun `saveIsForegroundServiceEnabled false then read back returns false`() = runTest {
        val c = controller()
        c.saveIsForegroundServiceEnabled(true)
        c.saveIsForegroundServiceEnabled(false)

        assertEquals(false, c.getIsForegroundServiceEnabled())
    }

    @Test
    fun `saveServerIp then getServerIp emits the saved value`() = runTest {
        val c = controller()
        c.saveServerIp("192.168.1.1")

        assertEquals("192.168.1.1", c.getServerIp().first())
    }

    @Test
    fun `saveCurrentAccount then getCurrentAccount roundtrips via json`() = runTest {
        val c = controller()
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())

        c.saveCurrentAccount(account)

        assertEquals(account, c.getCurrentAccount())
    }

    @Test
    fun `saveCurrentAccount with null removes the stored account`() = runTest {
        val c = controller()
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())
        c.saveCurrentAccount(account)

        c.saveCurrentAccount(null)

        assertNull(c.getCurrentAccount())
    }

    @Test
    fun `saveSelectedDevice then getSelectedDevice roundtrips via json`() = runTest {
        val c = controller()
        val device = Device(uuid = "device-1", name = "Device", description = "desc")

        c.saveSelectedDevice(device)

        assertEquals(device, c.getSelectedDevice())
    }

    @Test
    fun `getSelectedDevice returns null once clearAllCacheExceptDeviceUuid removes it`() = runTest {
        val c = controller()
        val device = Device(uuid = "device-1", name = "Device", description = "desc")
        c.saveSelectedDevice(device)

        // selected_device is not one of the keys clearAllCacheExceptDeviceUuid preserves,
        // so this deterministically empties it regardless of what other tests ran before.
        c.clearAllCacheExceptDeviceUuid()

        assertNull(c.getSelectedDevice())
    }

    @Test
    fun `saveAppSessionMode then getAppSessionMode returns the saved mode`() = runTest {
        val c = controller()
        c.saveAppSessionMode("server")

        assertEquals("server", c.getAppSessionMode())
    }

    @Test
    fun `saveAppSessionMode with null clears the app session mode`() = runTest {
        val c = controller()
        c.saveAppSessionMode("server")

        c.saveAppSessionMode(null)

        assertNull(c.getAppSessionMode())
    }

    @Test
    fun `clearAllCacheExceptDeviceUuid preserves device uuid server mode and server ip but clears the rest`() = runTest {
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
    fun `saveServerMode then getServerMode emits the saved mode`() = runTest {
        val c = controller()
        c.saveServerMode("local")

        assertEquals("local", c.getServerMode().first())
    }

    @Test
    fun `saveEmail does not throw`() = runTest {
        // No public getter exposes account_email; this only protects the write path from regressing.
        controller().saveEmail("user@example.com")
    }

    @Test
    fun `getLocalDeviceToken does not throw`() = runTest {
        // No public setter exists on this class for device_token; this only protects the read path.
        controller().getLocalDeviceToken()
    }
}
