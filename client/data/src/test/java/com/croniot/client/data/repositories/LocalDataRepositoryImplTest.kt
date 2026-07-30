package com.croniot.client.data.repositories

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.core.util.DevicePropertiesController
import com.croniot.client.data.source.local.AppPreferencesLocalDatasource
import com.croniot.client.data.source.local.AuthLocalDatasource
import com.croniot.client.data.source.local.DeviceLocalDatasource
import com.croniot.client.data.source.local.NavigationLocalDatasource
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LocalDataRepositoryImplTest {

    private val navigationLocalDatasource: NavigationLocalDatasource = mockk()
    private val authLocalDatasource: AuthLocalDatasource = mockk()
    private val deviceLocalDatasource: DeviceLocalDatasource = mockk()
    private val appPreferencesLocalDatasource: AppPreferencesLocalDatasource = mockk()
    private val serverConfigLocalDatasource: ServerConfigLocalDatasource = mockk()

    private lateinit var repository: LocalDataRepositoryImpl

    private val account = Account(
        uuid = "account-uuid",
        nickname = "nickname",
        email = "user@example.com",
        devices = emptyList(),
    )

    private val device = Device(
        uuid = "device-uuid",
        name = "device-name",
        description = "device-description",
        transport = TransportKind.CLOUD,
    )

    @BeforeEach
    fun setUp() {
        repository = LocalDataRepositoryImpl(
            navigationLocalDatasource,
            authLocalDatasource,
            deviceLocalDatasource,
            appPreferencesLocalDatasource,
            serverConfigLocalDatasource,
        )
    }

    @Test
    fun `getCurrentRoute delegates to navigation local datasource and returns its result`() = runTest {
        coEvery { navigationLocalDatasource.getCurrentRoute() } returns "home"

        val result = repository.getCurrentRoute()

        assertEquals("home", result)
        coVerify(exactly = 1) { navigationLocalDatasource.getCurrentRoute() }
    }

    @Test
    fun `saveCurrentRoute delegates route to navigation local datasource`() = runTest {
        coJustRun { navigationLocalDatasource.saveCurrentRoute(any()) }

        repository.saveCurrentRoute("settings")

        coVerify(exactly = 1) { navigationLocalDatasource.saveCurrentRoute("settings") }
    }

    @Test
    fun `getCurrentAccount delegates to auth local datasource and returns its result`() = runTest {
        coEvery { authLocalDatasource.getCurrentAccount() } returns account

        val result = repository.getCurrentAccount()

        assertEquals(account, result)
        coVerify(exactly = 1) { authLocalDatasource.getCurrentAccount() }
    }

    @Test
    fun `getCurrentAccount returns null when no account stored`() = runTest {
        coEvery { authLocalDatasource.getCurrentAccount() } returns null

        val result = repository.getCurrentAccount()

        assertNull(result)
    }

    @Test
    fun `generateAndSaveDeviceUuidIfNotExists delegates to device local datasource`() = runTest {
        coJustRun { deviceLocalDatasource.generateAndSaveDeviceUuidIfNotExists() }

        repository.generateAndSaveDeviceUuidIfNotExists()

        coVerify(exactly = 1) { deviceLocalDatasource.generateAndSaveDeviceUuidIfNotExists() }
    }

    @Test
    fun `getSelectedDevice delegates to device local datasource and returns its result`() = runTest {
        coEvery { deviceLocalDatasource.getSelectedDevice() } returns device

        val result = repository.getSelectedDevice()

        assertEquals(device, result)
        coVerify(exactly = 1) { deviceLocalDatasource.getSelectedDevice() }
    }

    @Test
    fun `saveSelectedDevice delegates device to device local datasource`() = runTest {
        coJustRun { deviceLocalDatasource.saveSelectedDevice(any()) }

        repository.saveSelectedDevice(device)

        coVerify(exactly = 1) { deviceLocalDatasource.saveSelectedDevice(device) }
    }

    @Test
    fun `clearAllCacheExceptDeviceUuid delegates to app preferences local datasource`() = runTest {
        coJustRun { appPreferencesLocalDatasource.clearAllCacheExceptDeviceUuid() }

        repository.clearAllCacheExceptDeviceUuid()

        coVerify(exactly = 1) { appPreferencesLocalDatasource.clearAllCacheExceptDeviceUuid() }
    }

    @Test
    fun `getDeviceProperties delegates to DevicePropertiesController`() {
        mockkObject(DevicePropertiesController)
        try {
            every { DevicePropertiesController.getDeviceDetails() } returns mapOf("Android Version" to "14")

            val result = repository.getDeviceProperties()

            assertEquals(mapOf("Android Version" to "14"), result)
        } finally {
            unmockkObject(DevicePropertiesController)
        }
    }

    @Test
    fun `getServerIp returns first value emitted by server config local datasource`() = runTest {
        coEvery { serverConfigLocalDatasource.getServerIp() } returns flowOf("1.2.3.4")

        val result = repository.getServerIp()

        assertEquals("1.2.3.4", result)
    }

    @Test
    fun `getIsForegroundServiceEnabled delegates to app preferences local datasource`() = runTest {
        coEvery { appPreferencesLocalDatasource.getIsForegroundServiceEnabled() } returns true

        val result = repository.getIsForegroundServiceEnabled()

        assertEquals(true, result)
    }

    @Test
    fun `saveIsForegroundServiceEnabled delegates flag to app preferences local datasource`() = runTest {
        coJustRun { appPreferencesLocalDatasource.saveIsForegroundServiceEnabled(any()) }

        repository.saveIsForegroundServiceEnabled(true)

        coVerify(exactly = 1) { appPreferencesLocalDatasource.saveIsForegroundServiceEnabled(true) }
    }

    @Test
    fun `getCurrentScreen delegates to navigation local datasource and returns its result`() = runTest {
        coEvery { navigationLocalDatasource.getCurrentScreen() } returns "HomeScreen"

        val result = repository.getCurrentScreen()

        assertEquals("HomeScreen", result)
    }

    @Test
    fun `saveCurrentScreen delegates screen to navigation local datasource`() = runTest {
        coJustRun { navigationLocalDatasource.saveCurrentScreen(any()) }

        repository.saveCurrentScreen("DetailScreen")

        coVerify(exactly = 1) { navigationLocalDatasource.saveCurrentScreen("DetailScreen") }
    }

    @Test
    fun `getLocalDeviceToken delegates to device local datasource and returns its result`() = runTest {
        coEvery { deviceLocalDatasource.getLocalDeviceToken() } returns "token-abc"

        val result = repository.getLocalDeviceToken()

        assertEquals("token-abc", result)
    }

    @Test
    fun `saveCurrentAccount delegates account to auth local datasource`() = runTest {
        coJustRun { authLocalDatasource.saveCurrentAccount(any()) }

        repository.saveCurrentAccount(account)

        coVerify(exactly = 1) { authLocalDatasource.saveCurrentAccount(account) }
    }

    @Test
    fun `saveCurrentAccount with null clears stored account`() = runTest {
        coJustRun { authLocalDatasource.saveCurrentAccount(any()) }

        repository.saveCurrentAccount(null)

        coVerify(exactly = 1) { authLocalDatasource.saveCurrentAccount(null) }
    }

    @Test
    fun `saveEmail delegates email to auth local datasource`() = runTest {
        coJustRun { authLocalDatasource.saveEmail(any()) }

        repository.saveEmail("user@example.com")

        coVerify(exactly = 1) { authLocalDatasource.saveEmail("user@example.com") }
    }

    @Test
    fun `getServerMode delegates to server config local datasource`() = runTest {
        every { serverConfigLocalDatasource.getServerMode() } returns flowOf("cloud")

        val result = repository.getServerMode().first()

        assertEquals("cloud", result)
    }

    @Test
    fun `getAppSessionMode delegates to app preferences local datasource`() = runTest {
        coEvery { appPreferencesLocalDatasource.getAppSessionMode() } returns "server"

        val result = repository.getAppSessionMode()

        assertEquals("server", result)
    }

    @Test
    fun `saveAppSessionMode delegates mode to app preferences local datasource`() = runTest {
        coJustRun { appPreferencesLocalDatasource.saveAppSessionMode(any()) }

        repository.saveAppSessionMode("ble")

        coVerify(exactly = 1) { appPreferencesLocalDatasource.saveAppSessionMode("ble") }
    }
}
