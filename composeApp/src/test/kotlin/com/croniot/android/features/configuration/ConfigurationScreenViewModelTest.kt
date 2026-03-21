package com.croniot.android.features.configuration

import com.croniot.client.data.source.local.LocalDatasource
import com.croniot.client.data.source.remote.http.HostSelectionInterceptor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var localDatasource: LocalDatasource
    private lateinit var hostInterceptor: HostSelectionInterceptor

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        localDatasource = mockk(relaxed = true)
        hostInterceptor = HostSelectionInterceptor()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubDatasource(
        foregroundEnabled: Boolean = false,
        serverIp: String? = null,
    ) {
        coEvery { localDatasource.getIsForegroundServiceEnabled() } returns foregroundEnabled
        coEvery { localDatasource.getServerIp() } returns flowOf(serverIp)
        coEvery { localDatasource.getCurrentServerMode() } returns flowOf(null)
    }

    private fun createViewModel(): ConfigurationScreenViewModel {
        return ConfigurationScreenViewModel(localDatasource, hostInterceptor)
    }

    // --- Initialization ---

    @Test
    fun init_loadsForegroundServiceFromDatasource() = runTest {
        stubDatasource(foregroundEnabled = true)

        val viewModel = createViewModel()

        assertEquals(true, viewModel.state.value.foregroundServiceEnabled)
    }

    @Test
    fun init_loadsServerIpFromDatasource() = runTest {
        stubDatasource(serverIp = "10.0.0.1")

        val viewModel = createViewModel()

        assertEquals("10.0.0.1", viewModel.state.value.serverIp)
    }

    @Test
    fun init_usesFallbackIpWhenDatasourceReturnsNull() = runTest {
        stubDatasource(serverIp = null)

        val viewModel = createViewModel()

        assertEquals("192.168.50.163", viewModel.state.value.serverIp)
    }

    // --- SetForegroundService ---

    @Test
    fun setForegroundService_enableUpdatesStateAndPersists() = runTest {
        stubDatasource(foregroundEnabled = false)

        val viewModel = createViewModel()
        viewModel.onIntent(ConfigurationIntent.SetForegroundService(true))

        assertEquals(true, viewModel.state.value.foregroundServiceEnabled)
        coVerify { localDatasource.saveIsForegroundServiceEnabled(true) }
    }

    @Test
    fun setForegroundService_disableUpdatesStateAndPersists() = runTest {
        stubDatasource(foregroundEnabled = true)

        val viewModel = createViewModel()
        viewModel.onIntent(ConfigurationIntent.SetForegroundService(false))

        assertEquals(false, viewModel.state.value.foregroundServiceEnabled)
        coVerify { localDatasource.saveIsForegroundServiceEnabled(false) }
    }

    // --- SetServerIp ---

    @Test
    fun setServerIp_updatesStateAndPersists() = runTest {
        stubDatasource(serverIp = "10.0.0.1")

        val viewModel = createViewModel()
        viewModel.onIntent(ConfigurationIntent.SetServerIp("192.168.1.50"))

        assertEquals("192.168.1.50", viewModel.state.value.serverIp)
        coVerify { localDatasource.saveServerIp("192.168.1.50") }
    }

    @Test
    fun setServerIp_updatesHostInterceptor() = runTest {
        stubDatasource()

        val viewModel = createViewModel()
        viewModel.onIntent(ConfigurationIntent.SetServerIp("10.0.0.5"))

        assertEquals("10.0.0.5", hostInterceptor.host)
    }
}