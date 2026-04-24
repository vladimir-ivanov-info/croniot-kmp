package com.croniot.android.features.configuration

import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.remote.http.HostHolder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigurationScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: ConfigurationScreenViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ConfigurationScreenViewModel(
            serverConfigLocalDatasource = mockk(relaxed = true),
            hostHolder = HostHolder(initial = ServerConfig.SERVER_IP_REMOTE)
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Initial state verification`() {
        val expectedState = ConfigurationState()
        val result = viewModel.state.value

        assertEquals(expectedState, result)
    }
}
