package com.croniot.android.core.presentation.splash

import Outcome
import com.croniot.android.app.AppError
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.domain.models.session.AppSession
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.testing.fakes.FakeAppSessionRepository
import com.croniot.testing.fakes.FakeLocalDataRepository
import com.croniot.testing.fakes.FakeSessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashScreenViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val localDataRepository = FakeLocalDataRepository(account = null, selectedDevice = null)
    private val sessionRepository = FakeSessionRepository()
    private val logOutUseCase = mockk<LogoutUseCase>(relaxed = true)
    private val startDeviceListenersUseCase = mockk<StartDeviceListenersUseCase>()
    private val appSessionRepository = FakeAppSessionRepository()

    private lateinit var viewModel: SplashScreenViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SplashScreenViewModel(
            localDataRepository = localDataRepository,
            sessionRepository = sessionRepository,
            logOutUseCase = logOutUseCase,
            startDeviceListenersUseCase = startDeviceListenersUseCase,
            appSessionRepository = appSessionRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Sesión inválida → logout + NavigateToLogin ---

    @Test
    fun `WHEN initSplash is called with no account THEN it logs out and navigates to login`() = runTest(testDispatcher) {
        sessionRepository.saveTokens(validTokens())
        val effects = collectEffects()

        viewModel.initSplash()

        assertEquals(listOf(SplashEffect.NavigateToLogin), effects)
        coVerify { logOutUseCase() }
        assertEquals(AppSession.None, appSessionRepository.session.value)
    }

    @Test
    fun `WHEN initSplash is called with no tokens THEN it navigates to login`() = runTest(testDispatcher) {
        localDataRepository.saveCurrentAccount(account())
        val effects = collectEffects()

        viewModel.initSplash()

        assertEquals(listOf(SplashEffect.NavigateToLogin), effects)
        coVerify { logOutUseCase() }
        assertEquals(AppSession.None, appSessionRepository.session.value)
    }

    @Test
    fun `WHEN initSplash is called with an expired access token THEN it navigates to login`() = runTest(testDispatcher) {
        localDataRepository.saveCurrentAccount(account())
        sessionRepository.saveTokens(expiredTokens())
        val effects = collectEffects()

        viewModel.initSplash()

        assertEquals(listOf(SplashEffect.NavigateToLogin), effects)
        coVerify { logOutUseCase() }
        assertEquals(AppSession.None, appSessionRepository.session.value)
    }

    // --- Sesión válida + listeners OK ---

    @Test
    fun `WHEN initSplash is called with a valid session, listeners ok, and a selected device THEN it navigates to that device without an error`() =
        runTest(testDispatcher) {
            val account = account()
            localDataRepository.saveCurrentAccount(account)
            sessionRepository.saveTokens(validTokens())
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Ok(Unit)
            localDataRepository.saveSelectedDevice(device("device-1"))
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDevice("device-1", null)), effects)
            coVerify(exactly = 0) { logOutUseCase() }
            assertEquals(AppSession.Server(account), appSessionRepository.session.value)
        }

    @Test
    fun `WHEN initSplash is called with a valid session, listeners ok, and no selected device THEN it navigates to the device list without an error`() =
        runTest(testDispatcher) {
            val account = account()
            localDataRepository.saveCurrentAccount(account)
            sessionRepository.saveTokens(validTokens())
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Ok(Unit)
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDeviceList(null)), effects)
            assertEquals(AppSession.Server(account), appSessionRepository.session.value)
        }

    // --- Sesión válida + listeners con error → arrastra AppError ---

    @Test
    fun `WHEN initSplash is called with a valid session, a listeners error, and a selected device THEN it navigates to that device with the app error`() =
        runTest(testDispatcher) {
            localDataRepository.saveCurrentAccount(account())
            sessionRepository.saveTokens(validTokens())
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Err(connectionErrors)
            localDataRepository.saveSelectedDevice(device("device-1"))
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDevice("device-1", expectedAppError)), effects)
        }

    @Test
    fun `WHEN initSplash is called with a valid session, a listeners error, and no selected device THEN it navigates to the device list with the app error`() =
        runTest(testDispatcher) {
            localDataRepository.saveCurrentAccount(account())
            sessionRepository.saveTokens(validTokens())
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Err(connectionErrors)
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDeviceList(expectedAppError)), effects)
        }

    // --- Helpers ---

    private fun TestScope.collectEffects(): List<SplashEffect> {
        val sink = mutableListOf<SplashEffect>()
        backgroundScope.launch(testDispatcher) { viewModel.effects.toList(sink) }
        return sink
    }

    private fun nowSeconds() = System.currentTimeMillis() / 1000L
    private fun validTokens() = AuthTokens("access", "refresh", expiresAtEpochSeconds = nowSeconds() + 3600)
    private fun expiredTokens() = AuthTokens("access", "refresh", expiresAtEpochSeconds = nowSeconds() - 3600)
    private fun device(uuid: String) = Device(uuid = uuid, name = "name", description = "desc")
    private fun account(devices: List<Device> = listOf(device("device-1"))) =
        Account(uuid = "account-uuid", nickname = "nick", email = "user@croniot.com", devices = devices)

    private val connectionErrors = listOf(
        ConnectionError.MqttBrokerUnreachable(host = "broker-1", cause = null),
        ConnectionError.Unknown,
    )
    private val expectedAppError = AppError(
        title = "Error de conexión",
        message = "No se pudo conectar al broker MQTT (broker-1).\nError de conexión desconocido.",
    )
}
