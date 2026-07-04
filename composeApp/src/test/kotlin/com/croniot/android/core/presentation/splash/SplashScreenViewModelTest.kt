package com.croniot.android.core.presentation.splash

import Outcome
import com.croniot.android.app.AppError
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.Device
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.repositories.SessionRepository
import com.croniot.client.domain.usecases.LogoutUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
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

    private val localDataRepository = mockk<LocalDataRepository>()
    private val sessionRepository = mockk<SessionRepository>()
    private val logOutUseCase = mockk<LogoutUseCase>(relaxed = true)
    private val startDeviceListenersUseCase = mockk<StartDeviceListenersUseCase>()
    private val appSessionRepository = mockk<AppSessionRepository>(relaxed = true)

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
    fun `given no account when initSplash then logs out and navigates to login`() = runTest(testDispatcher) {
        coEvery { localDataRepository.getCurrentAccount() } returns null
        coEvery { sessionRepository.getTokens() } returns validTokens()
        val effects = collectEffects()

        viewModel.initSplash()

        assertEquals(listOf(SplashEffect.NavigateToLogin), effects)
        coVerify { logOutUseCase() }
    }

    @Test
    fun `given no tokens when initSplash then navigates to login`() = runTest(testDispatcher) {
        coEvery { localDataRepository.getCurrentAccount() } returns account()
        coEvery { sessionRepository.getTokens() } returns null
        val effects = collectEffects()

        viewModel.initSplash()

        assertEquals(listOf(SplashEffect.NavigateToLogin), effects)
        coVerify { logOutUseCase() }
    }

    @Test
    fun `given expired access token when initSplash then navigates to login`() = runTest(testDispatcher) {
        coEvery { localDataRepository.getCurrentAccount() } returns account()
        coEvery { sessionRepository.getTokens() } returns expiredTokens()
        val effects = collectEffects()

        viewModel.initSplash()

        assertEquals(listOf(SplashEffect.NavigateToLogin), effects)
        coVerify { logOutUseCase() }
    }

    // --- Sesión válida + listeners OK ---

    @Test
    fun `given valid session and listeners ok and selected device then navigates to that device without error`() =
        runTest(testDispatcher) {
            coEvery { localDataRepository.getCurrentAccount() } returns account()
            coEvery { sessionRepository.getTokens() } returns validTokens()
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Ok(Unit)
            coEvery { localDataRepository.getSelectedDevice() } returns device("device-1")
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDevice("device-1", null)), effects)
            coVerify(exactly = 0) { logOutUseCase() }
        }

    @Test
    fun `given valid session and listeners ok and no selected device then navigates to device list without error`() =
        runTest(testDispatcher) {
            coEvery { localDataRepository.getCurrentAccount() } returns account()
            coEvery { sessionRepository.getTokens() } returns validTokens()
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Ok(Unit)
            coEvery { localDataRepository.getSelectedDevice() } returns null
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDeviceList(null)), effects)
        }

    // --- Sesión válida + listeners con error → arrastra AppError ---

    @Test
    fun `given valid session and listeners error and selected device then navigates to that device with app error`() =
        runTest(testDispatcher) {
            coEvery { localDataRepository.getCurrentAccount() } returns account()
            coEvery { sessionRepository.getTokens() } returns validTokens()
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Err(connectionErrors)
            coEvery { localDataRepository.getSelectedDevice() } returns device("device-1")
            val effects = collectEffects()

            viewModel.initSplash()

            assertEquals(listOf(SplashEffect.NavigateToDevice("device-1", expectedAppError)), effects)
        }

    @Test
    fun `given valid session and listeners error and no selected device then navigates to device list with app error`() =
        runTest(testDispatcher) {
            coEvery { localDataRepository.getCurrentAccount() } returns account()
            coEvery { sessionRepository.getTokens() } returns validTokens()
            coEvery { startDeviceListenersUseCase(any()) } returns Outcome.Err(connectionErrors)
            coEvery { localDataRepository.getSelectedDevice() } returns null
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