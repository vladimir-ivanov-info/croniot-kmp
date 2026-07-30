package com.croniot.android.features.device.presentation

import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.usecases.FetchTasksUseCase
import com.croniot.client.domain.usecases.GetDeviceUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.domain.usecases.ble.ObserveBleRssiUseCase
import com.croniot.client.domain.models.Account
import com.croniot.testing.fakes.FakeLocalDataRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val localDataRepository = FakeLocalDataRepository()
    private val fetchTasksUseCase: FetchTasksUseCase = mockk(relaxed = true)
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase = mockk(relaxed = true)
    private val getDeviceUseCase: GetDeviceUseCase = mockk()
    private val observeBleRssiUseCase: ObserveBleRssiUseCase = mockk(relaxed = true)

    private lateinit var viewModel: DeviceScreenViewModel

    private fun cloudDevice(uuid: String = "device-1") = Device(
        uuid = uuid,
        name = "Device 1",
        description = "desc",
        transport = TransportKind.CLOUD,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DeviceScreenViewModel(
            localDataRepository = localDataRepository,
            fetchTasksUseCase = fetchTasksUseCase,
            startDeviceListenersUseCase = startDeviceListenersUseCase,
            getDeviceUseCase = getDeviceUseCase,
            observeBleRssiUseCase = observeBleRssiUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Initial state verification`() {
        val result = viewModel.state.value

        assertTrue(result is DeviceState.Loading)
    }

    @Test
    fun `Given device found, When Initialize, Then state is Content and listeners and tasks are triggered`() =
        runTest {
            val device = cloudDevice()
            coEvery { getDeviceUseCase(device.uuid) } returns device

            viewModel.onIntent(DeviceIntent.Initialize(device.uuid))

            val result = viewModel.state.value
            assertTrue(result is DeviceState.Content)
            assertEquals(device, (result as DeviceState.Content).device)
            coVerify(exactly = 1) { startDeviceListenersUseCase(listOf(device)) }
            coVerify(exactly = 1) { fetchTasksUseCase(device.uuid) }
        }

    @Test
    fun `Given device not found, When Initialize, Then state is Error`() = runTest {
        coEvery { getDeviceUseCase("missing-uuid") } returns null

        viewModel.onIntent(DeviceIntent.Initialize("missing-uuid"))

        val result = viewModel.state.value
        assertTrue(result is DeviceState.Error)
    }

    @Test
    fun `Given already in Content for the same uuid, When Initialize is called again, Then does not reload device`() =
        runTest {
            val device = cloudDevice()
            coEvery { getDeviceUseCase(device.uuid) } returns device

            viewModel.onIntent(DeviceIntent.Initialize(device.uuid))
            viewModel.onIntent(DeviceIntent.Initialize(device.uuid))

            coVerify(exactly = 1) { getDeviceUseCase(device.uuid) }
        }

    @Test
    fun `Given Content state, When SelectTab, Then updates selectedTab keeping rest of the state`() = runTest {
        val device = cloudDevice()
        coEvery { getDeviceUseCase(device.uuid) } returns device
        viewModel.onIntent(DeviceIntent.Initialize(device.uuid))

        viewModel.onIntent(DeviceIntent.SelectTab(2))

        val result = viewModel.state.value
        assertTrue(result is DeviceState.Content)
        val content = result as DeviceState.Content
        assertEquals(2, content.selectedTab)
        assertEquals(device, content.device)
        assertEquals(null, content.rssi)
    }

    @Test
    fun `Given a BLE device, When Initialize, Then observes rssi and updates state`() = runTest {
        val device = cloudDevice().copy(transport = TransportKind.BLE)
        coEvery { getDeviceUseCase(device.uuid) } returns device
        coEvery { observeBleRssiUseCase(device.uuid) } returns flowOf(-50)

        viewModel.onIntent(DeviceIntent.Initialize(device.uuid))

        val result = viewModel.state.value
        assertTrue(result is DeviceState.Content)
        assertEquals(-50, (result as DeviceState.Content).rssi)
    }

    @Test
    fun `Given Loading state, When SelectTab, Then state remains Loading`() = runTest {
        viewModel.onIntent(DeviceIntent.SelectTab(2))

        assertTrue(viewModel.state.value is DeviceState.Loading)
    }

    @Test
    fun `Given a stored account, When reconnectIfNeeded, Then starts device listeners with account devices`() =
        runTest {
            val device = cloudDevice()
            val account = Account(uuid = "acc-1", nickname = "nick", email = "user@test.com", devices = listOf(device))
            val repoWithAccount = FakeLocalDataRepository(account = account)
            val vm = DeviceScreenViewModel(
                localDataRepository = repoWithAccount,
                fetchTasksUseCase = fetchTasksUseCase,
                startDeviceListenersUseCase = startDeviceListenersUseCase,
                getDeviceUseCase = getDeviceUseCase,
                observeBleRssiUseCase = observeBleRssiUseCase,
            )

            vm.reconnectIfNeeded()

            coVerify(exactly = 1) { startDeviceListenersUseCase(listOf(device)) }
        }

    @Test
    fun `Given no stored account, When reconnectIfNeeded, Then does not start device listeners`() = runTest {
        viewModel.reconnectIfNeeded()

        coVerify(exactly = 0) { startDeviceListenersUseCase(any()) }
    }
}
