package com.croniot.client.features.blediscovery.presentation

import com.croniot.client.data.source.remote.ble.BlePermissionsHelper
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.usecases.StopDeviceListenersUseCase
import com.croniot.client.domain.usecases.ble.ActivateBleOnlyModeUseCase
import com.croniot.client.domain.usecases.ble.ConnectBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ForgetBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ObserveKnownBleDevicesUseCase
import com.croniot.client.domain.usecases.ble.PairBleDeviceUseCase
import com.croniot.client.domain.usecases.ble.ScanBleDevicesUseCase
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeBleDevicesRepository
import com.croniot.testing.fakes.FakeSensorDataRepository
import com.croniot.testing.fakes.FakeTasksRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BleDiscoveryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val device = Device(uuid = "device-1", name = "Device 1", description = "")
    private val permissionsHelper: BlePermissionsHelper = mockk()

    private lateinit var bleDevicesRepository: FakeBleDevicesRepository
    private lateinit var appSessionRepository: FakeAppSessionRepository
    private lateinit var viewModel: BleDiscoveryViewModel

    private fun buildViewModel(devicesByUuid: Map<String, Device> = emptyMap()) {
        bleDevicesRepository = FakeBleDevicesRepository(devicesByUuid = devicesByUuid)
        appSessionRepository = FakeAppSessionRepository(initial = AppSession.None)
        val activateBleOnlyModeUseCase = ActivateBleOnlyModeUseCase(
            stopDeviceListenersUseCase = StopDeviceListenersUseCase(FakeSensorDataRepository(), FakeTasksRepository()),
            appSessionRepository = appSessionRepository,
        )
        viewModel = BleDiscoveryViewModel(
            scanBleDevicesUseCase = ScanBleDevicesUseCase(bleDevicesRepository),
            observeKnownBleDevicesUseCase = ObserveKnownBleDevicesUseCase(bleDevicesRepository),
            pairBleDeviceUseCase = PairBleDeviceUseCase(bleDevicesRepository),
            connectBleDeviceUseCase = ConnectBleDeviceUseCase(bleDevicesRepository),
            forgetBleDeviceUseCase = ForgetBleDeviceUseCase(bleDevicesRepository),
            activateBleOnlyModeUseCase = activateBleOnlyModeUseCase,
            permissionsHelper = permissionsHelper,
        )
    }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { permissionsHelper.allGranted() } returns true
        every { permissionsHelper.missingPermissions() } returns emptyList()
        buildViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects permission status from helper`() = runTest(testDispatcher) {
        assertTrue(viewModel.state.value.permissionsGranted)
        assertTrue(viewModel.state.value.missingPermissions.isEmpty())
    }

    @Test
    fun `PermissionsGranted intent marks permissions as granted`() = runTest(testDispatcher) {
        every { permissionsHelper.allGranted() } returns false
        every { permissionsHelper.missingPermissions() } returns listOf("BLUETOOTH_SCAN")
        buildViewModel()

        viewModel.onAction(BleDiscoveryIntent.PermissionsGranted)

        assertTrue(viewModel.state.value.permissionsGranted)
        assertTrue(viewModel.state.value.missingPermissions.isEmpty())
    }

    @Test
    fun `RefreshPermissionStatus intent re-reads permission status from helper`() = runTest(testDispatcher) {
        every { permissionsHelper.allGranted() } returns false
        every { permissionsHelper.missingPermissions() } returns listOf("BLUETOOTH_CONNECT")

        viewModel.onAction(BleDiscoveryIntent.RefreshPermissionStatus)

        assertFalse(viewModel.state.value.permissionsGranted)
        assertEquals(listOf("BLUETOOTH_CONNECT"), viewModel.state.value.missingPermissions)
    }

    @Test
    fun `PairRequested shows the pairing dialog with device uuid and display name`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.PairRequested(uuid = "device-1", displayName = "My Device"))

        val pairing = viewModel.state.value.pairing
        assertEquals("device-1", pairing?.uuid)
        assertEquals("My Device", pairing?.displayName)
    }

    @Test
    fun `PairDialogDismissed clears the pairing state`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.PairRequested(uuid = "device-1", displayName = "My Device"))

        viewModel.onAction(BleDiscoveryIntent.PairDialogDismissed)

        assertNull(viewModel.state.value.pairing)
    }

    @Test
    fun `UsernameChanged updates the pairing username`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.PairRequested(uuid = "device-1", displayName = "My Device"))

        viewModel.onAction(BleDiscoveryIntent.UsernameChanged("new-user"))

        assertEquals("new-user", viewModel.state.value.pairing?.username)
    }

    @Test
    fun `PasswordChanged updates the pairing password`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.PairRequested(uuid = "device-1", displayName = "My Device"))

        viewModel.onAction(BleDiscoveryIntent.PasswordChanged("new-pass"))

        assertEquals("new-pass", viewModel.state.value.pairing?.password)
    }

    @Test
    fun `PairConfirmed on success clears pairing state and activates ble only mode`() = runTest(testDispatcher) {
        buildViewModel(devicesByUuid = mapOf("device-1" to device))
        viewModel.onAction(BleDiscoveryIntent.PairRequested(uuid = "device-1", displayName = "My Device"))

        viewModel.onAction(BleDiscoveryIntent.PairConfirmed)

        assertNull(viewModel.state.value.pairing)
        assertEquals(AppSession.BleOnly, appSessionRepository.session.value)
    }

    @Test
    fun `PairConfirmed on failure keeps dialog open with error message`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.PairRequested(uuid = "unknown-device", displayName = "Unknown"))

        viewModel.onAction(BleDiscoveryIntent.PairConfirmed)

        assertEquals(false, viewModel.state.value.pairing?.isSubmitting)
        assertTrue(viewModel.state.value.pairing?.error != null)
    }

    @Test
    fun `PairConfirmed with no pairing state does nothing`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.PairConfirmed)

        assertNull(viewModel.state.value.pairing)
    }

    @Test
    fun `ConnectKnown on success clears busy state and activates ble only mode`() = runTest(testDispatcher) {
        buildViewModel(devicesByUuid = mapOf("device-1" to device))

        viewModel.onAction(BleDiscoveryIntent.ConnectKnown("device-1"))

        assertNull(viewModel.state.value.busyUuid)
        assertEquals(AppSession.BleOnly, appSessionRepository.session.value)
    }

    @Test
    fun `ConnectKnown on failure clears busy state without activating ble only mode`() = runTest(testDispatcher) {
        viewModel.onAction(BleDiscoveryIntent.ConnectKnown("unknown-device"))

        assertNull(viewModel.state.value.busyUuid)
        assertEquals(AppSession.None, appSessionRepository.session.value)
    }

    @Test
    fun `ForgetKnown removes the device from the repository`() = runTest(testDispatcher) {
        buildViewModel(devicesByUuid = mapOf("device-1" to device))

        viewModel.onAction(BleDiscoveryIntent.ForgetKnown("device-1"))

        assertEquals(1, bleDevicesRepository.forgetCalls)
    }
}
