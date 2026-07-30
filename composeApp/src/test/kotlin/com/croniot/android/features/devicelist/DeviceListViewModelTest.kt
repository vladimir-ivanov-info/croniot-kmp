package com.croniot.android.features.devicelist

import com.croniot.android.core.notifications.TaskNotificationManager
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.TransportKind
import com.croniot.client.domain.models.ble.KnownBleDevice
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.domain.usecases.ble.ForgetBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ObserveKnownBleDevicesUseCase
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeLocalDataRepository
import com.croniot.testing.fakes.FakeSensorDataRepository
import com.croniot.testing.fakes.FakeTasksRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Only the deterministic behaviours of [DeviceListViewModel] are covered here.
 *
 * The `init` block combines two chained flows (`appSessionRepository.session` and the
 * per-device `sensorDataRepository.devicesLatestSensorTimestamp` subscriptions started by
 * `resubscribeToDevices`). With [UnconfinedTestDispatcher] both settle synchronously right
 * after the view model is constructed, which is why every test below can read `state.value`
 * immediately without needing `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val localDataRepository = FakeLocalDataRepository()
    private val sensorDataRepository = FakeSensorDataRepository()
    private val tasksRepository = FakeTasksRepository()
    private val logOutUseCase: LogoutUseCase = mockk(relaxed = true)
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase = mockk(relaxed = true)
    private val taskNotificationManager: TaskNotificationManager = mockk(relaxed = true)
    private val observeKnownBleDevicesUseCase: ObserveKnownBleDevicesUseCase = mockk()
    private val forgetBleDeviceUseCase: ForgetBleDeviceUseCase = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { observeKnownBleDevicesUseCase() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(session: AppSession): DeviceListViewModel {
        return DeviceListViewModel(
            localDataRepository = localDataRepository,
            sensorDataRepository = sensorDataRepository,
            tasksRepository = tasksRepository,
            logOutUseCase = logOutUseCase,
            startDeviceListenersUseCase = startDeviceListenersUseCase,
            taskNotificationManager = taskNotificationManager,
            appSessionRepository = FakeAppSessionRepository(initial = session),
            observeKnownBleDevicesUseCase = observeKnownBleDevicesUseCase,
            forgetBleDeviceUseCase = forgetBleDeviceUseCase,
        )
    }

    private fun device(uuid: String, name: String) = Device(uuid = uuid, name = name, description = "")

    @Test
    fun `Given server session with some unnamed devices, When created, Then only named devices are kept as CLOUD`() =
        runTest {
            val devices = listOf(device("d1", "Device 1"), device("d2", ""), device("d3", "Device 3"))
            val account = Account(uuid = "acc-1", nickname = "nick", email = "user@test.com", devices = devices)

            val viewModel = buildViewModel(AppSession.Server(account))

            assertEquals(listOf(devices[0], devices[2]), viewModel.state.value.devices)
            assertEquals(TransportKind.CLOUD, viewModel.state.value.mode)
        }

    @Test
    fun `Given ble only session, When created, Then devices are mapped as BLE and inRangeUuids only keeps in-range uuids`() =
        runTest {
            val known = listOf(
                KnownBleDevice(uuid = "ble-1", displayName = "Ble 1", lastSeenAtMillis = 0, isInRange = true),
                KnownBleDevice(uuid = "ble-2", displayName = "Ble 2", lastSeenAtMillis = 0, isInRange = false),
            )
            every { observeKnownBleDevicesUseCase() } returns flowOf(known)

            val viewModel = buildViewModel(AppSession.BleOnly)

            assertEquals(2, viewModel.state.value.devices.size)
            assertTrue(viewModel.state.value.devices.all { it.transport == TransportKind.BLE })
            assertEquals(setOf("ble-1"), viewModel.state.value.inRangeUuids)
        }

    @Test
    fun `Given no session, When created, Then devices is empty`() = runTest {
        val viewModel = buildViewModel(AppSession.None)

        assertTrue(viewModel.state.value.devices.isEmpty())
    }

    @Test
    fun `Given a device disappears from the account, When session updates, Then stops listening for it and clears its lastSeenMillis`() =
        runTest {
            val devices = listOf(device("d1", "Device 1"), device("d2", "Device 2"))
            val account = Account(uuid = "acc-1", nickname = "nick", email = "user@test.com", devices = devices)
            val fakeSessionRepository = FakeAppSessionRepository(initial = AppSession.Server(account))
            val viewModel = DeviceListViewModel(
                localDataRepository = localDataRepository,
                sensorDataRepository = sensorDataRepository,
                tasksRepository = tasksRepository,
                logOutUseCase = logOutUseCase,
                startDeviceListenersUseCase = startDeviceListenersUseCase,
                taskNotificationManager = taskNotificationManager,
                appSessionRepository = fakeSessionRepository,
                observeKnownBleDevicesUseCase = observeKnownBleDevicesUseCase,
                forgetBleDeviceUseCase = forgetBleDeviceUseCase,
            )
            assertEquals(listOf("d1", "d2"), viewModel.state.value.devices.map { it.uuid })

            // "d2" drops out of the account -> resubscribeToDevices must cancel its collector,
            // stop listening for it on both repositories, and clear its lastSeenMillis entry.
            fakeSessionRepository.activateServerSession(account.copy(devices = listOf(devices[0])))

            assertEquals(listOf("d1"), viewModel.state.value.devices.map { it.uuid })
            assertTrue(sensorDataRepository.stopListeningForInvocations.contains("d2"))
            assertTrue(tasksRepository.stopListeningForInvocations.contains("d2"))
            assertTrue(!viewModel.state.value.lastSeenMillis.containsKey("d2"))
        }

    @Test
    fun `Given LogOut intent, When onIntent, Then stops notifications, logs out and emits LogOut effect`() =
        runTest {
            val viewModel = buildViewModel(AppSession.None)
            val effects = mutableListOf<DeviceListEffect>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effects.collect { effects.add(it) }
            }

            viewModel.onIntent(DeviceListIntent.LogOut)

            coVerify(exactly = 1) { taskNotificationManager.stopAll() }
            coVerify(exactly = 1) { logOutUseCase() }
            assertEquals(listOf(DeviceListEffect.LogOut), effects)
            job.cancel()
        }

    @Test
    fun `Given DeviceClicked intent, When onIntent, Then emits NavigateToDevice effect`() = runTest {
        val viewModel = buildViewModel(AppSession.None)
        val effects = mutableListOf<DeviceListEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onIntent(DeviceListIntent.DeviceClicked("device-uuid"))

        assertEquals(listOf(DeviceListEffect.NavigateToDevice("device-uuid")), effects)
        job.cancel()
    }

    @Test
    fun `Given ForgetBleDevice intent, When onIntent, Then calls forgetBleDeviceUseCase`() = runTest {
        val viewModel = buildViewModel(AppSession.None)

        viewModel.onIntent(DeviceListIntent.ForgetBleDevice("device-uuid"))

        coVerify(exactly = 1) { forgetBleDeviceUseCase("device-uuid") }
    }

    @Test
    fun `Given GoToBleDiscovery intent, When onIntent, Then emits NavigateToBleDiscovery effect`() = runTest {
        val viewModel = buildViewModel(AppSession.None)
        val effects = mutableListOf<DeviceListEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onIntent(DeviceListIntent.GoToBleDiscovery)

        assertEquals(listOf(DeviceListEffect.NavigateToBleDiscovery), effects)
        job.cancel()
    }

    @Test
    fun `Given CLOUD mode with a stored account, When reconnectIfNeeded, Then starts device listeners`() = runTest {
        val devices = listOf(device("d1", "Device 1"))
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@test.com", devices = devices)
        val localRepo = FakeLocalDataRepository(account = account)
        val viewModel = DeviceListViewModel(
            localDataRepository = localRepo,
            sensorDataRepository = sensorDataRepository,
            tasksRepository = tasksRepository,
            logOutUseCase = logOutUseCase,
            startDeviceListenersUseCase = startDeviceListenersUseCase,
            taskNotificationManager = taskNotificationManager,
            appSessionRepository = FakeAppSessionRepository(initial = AppSession.Server(account)),
            observeKnownBleDevicesUseCase = observeKnownBleDevicesUseCase,
            forgetBleDeviceUseCase = forgetBleDeviceUseCase,
        )

        viewModel.reconnectIfNeeded()

        coVerify(exactly = 1) { startDeviceListenersUseCase(devices) }
    }

    @Test
    fun `Given BLE mode, When reconnectIfNeeded, Then does not start device listeners`() = runTest {
        val viewModel = buildViewModel(AppSession.BleOnly)

        viewModel.reconnectIfNeeded()

        coVerify(exactly = 0) { startDeviceListenersUseCase(any()) }
    }

    @Test
    fun `Given CLOUD mode with no stored account, When reconnectIfNeeded, Then does not start device listeners`() =
        runTest {
            val account = Account(uuid = "acc-1", nickname = "nick", email = "user@test.com", devices = emptyList())
            val localRepo = FakeLocalDataRepository(account = null)
            val viewModel = DeviceListViewModel(
                localDataRepository = localRepo,
                sensorDataRepository = sensorDataRepository,
                tasksRepository = tasksRepository,
                logOutUseCase = logOutUseCase,
                startDeviceListenersUseCase = startDeviceListenersUseCase,
                taskNotificationManager = taskNotificationManager,
                appSessionRepository = FakeAppSessionRepository(initial = AppSession.Server(account)),
                observeKnownBleDevicesUseCase = observeKnownBleDevicesUseCase,
                forgetBleDeviceUseCase = forgetBleDeviceUseCase,
            )

            viewModel.reconnectIfNeeded()

            coVerify(exactly = 0) { startDeviceListenersUseCase(any()) }
        }
}
