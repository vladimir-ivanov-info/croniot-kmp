package com.croniot.android.features.configuration

import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.local.ServerConfigLocalDatasource
import com.croniot.client.data.source.remote.http.HostHolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var viewModel: ConfigurationScreenViewModel
    private val serverConfigLocalDatasource: ServerConfigLocalDatasource = mockk(relaxed = true)
    private lateinit var hostHolder: HostHolder

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        hostHolder = HostHolder(initial = ServerConfig.SERVER_IP_REMOTE)
        viewModel = ConfigurationScreenViewModel(
            serverConfigLocalDatasource = serverConfigLocalDatasource,
            hostHolder = hostHolder
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN the ViewModel is created THEN the state matches the default ConfigurationState`() {
        val expectedState = ConfigurationState()
        val result = viewModel.state.value

        assertEquals(expectedState, result)
    }

    @Test
    fun `WHEN SetServerIp is dispatched THEN the state is updated`() = runTest {
        viewModel.onIntent(ConfigurationIntent.SetServerIp("192.168.1.1"))

        assertEquals("192.168.1.1", viewModel.state.value.serverIp)
    }

    @Test
    fun `WHEN SetServerIp is dispatched THEN it saves to the datasource`() = runTest {
        viewModel.onIntent(ConfigurationIntent.SetServerIp("10.0.0.5"))

        coVerify(exactly = 1) { serverConfigLocalDatasource.saveServerIp("10.0.0.5") }
    }

    @Test
    fun `WHEN SetServerIp is dispatched THEN it updates HostHolder`() = runTest {
        viewModel.onIntent(ConfigurationIntent.SetServerIp("172.16.0.1"))

        assertEquals("172.16.0.1", hostHolder.host)
    }

    @Test
    fun `WHEN the ViewModel is initialized with a stored server ip THEN it loads it into the state`() = runTest {
        val datasource: ServerConfigLocalDatasource = mockk(relaxed = true)
        coEvery { datasource.getServerIp() } returns flowOf("10.20.30.40")

        val vm = ConfigurationScreenViewModel(serverConfigLocalDatasource = datasource, hostHolder = hostHolder)

        assertEquals("10.20.30.40", vm.state.value.serverIp)
    }

    @Test
    fun `WHEN the stored ip is null THEN the ViewModel falls back to the remote default ip`() = runTest {
        val datasource: ServerConfigLocalDatasource = mockk(relaxed = true)
        coEvery { datasource.getServerIp() } returns flowOf(null)

        val vm = ConfigurationScreenViewModel(serverConfigLocalDatasource = datasource, hostHolder = hostHolder)

        assertEquals(ServerConfig.SERVER_IP_REMOTE, vm.state.value.serverIp)
    }
}
