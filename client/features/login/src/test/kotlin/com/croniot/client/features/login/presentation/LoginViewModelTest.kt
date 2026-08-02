package com.croniot.client.features.login.presentation

import Outcome
import androidx.lifecycle.SavedStateHandle
import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.ConnectionError
import com.croniot.client.domain.models.auth.AuthError
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.LogInUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loginUseCase: LogInUseCase
    private lateinit var localDataRepository: LocalDataRepository
    private lateinit var startDeviceListenersUseCase: StartDeviceListenersUseCase
    private lateinit var appSessionRepository: AppSessionRepository
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        localDataRepository = mockk()
        startDeviceListenersUseCase = mockk()
        appSessionRepository = mockk(relaxed = true)

        viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            localDataRepository = localDataRepository,
            startDeviceListenersUseCase = startDeviceListenersUseCase,
            appSessionRepository = appSessionRepository,
            savedStateHandle = SavedStateHandle(),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `WHEN the ViewModel is created THEN isLoading is false`() = runTest {
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `WHEN EmailChanged is dispatched THEN the email in state is updated`() = runTest {
        val newEmail = "test@example.com"
        viewModel.onAction(LoginIntent.EmailChanged(newEmail))

        assertEquals(newEmail, viewModel.state.value.email)
    }

    @Test
    fun `WHEN PasswordChanged is dispatched THEN the password in state is updated`() = runTest {
        val newPassword = "new_password"
        viewModel.onAction(LoginIntent.PasswordChanged(newPassword))

        assertEquals(newPassword, viewModel.state.value.password)
    }

    @Test
    fun `WHEN Login succeeds THEN it navigates home`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Ok(Unit)
        coEvery { localDataRepository.getCurrentAccount() } returns null

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, effects.size)
        assertEquals(LoginEffect.NavigateHome, effects.first())
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with invalid credentials THEN it shows a snackbar with the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.InvalidCredentials)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, effects.size)
        val effect = effects.first()
        assertTrue(effect is LoginEffect.ShowSnackbar)
        val snackbar = effect as LoginEffect.ShowSnackbar
        assertEquals("Login failed", snackbar.title)
        assertEquals("Credenciales inválidas.", snackbar.content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with a Network error THEN it shows the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.Network)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("No hay conexión con el servidor.", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with a NetworkTiemout error THEN it shows the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.NetworkTiemout)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("Timeout con el servidor.", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with a DeviceMissing error THEN it shows the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.DeviceMissing)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals(
            "No se encontró el identificador del dispositivo.",
            (effects.first() as LoginEffect.ShowSnackbar).content,
        )
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with a Server error that carries a message THEN it shows that message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.Server("Custom server error"))

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("Custom server error", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with a Server error without a message THEN it falls back to the default message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.Server(null))

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("Error de servidor.", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with an Unknown error THEN it shows the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.Unknown)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("Error desconocido.", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with an AccountMissing error THEN it shows the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.AccountMissing)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("No account returned", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN Login fails with a TokenMissing error THEN it shows the correct message`() = runTest {
        coEvery { loginUseCase(any(), any()) } returns Outcome.Err(AuthError.TokenMissing)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals("No token returned", (effects.first() as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `WHEN GoToCreateAccountScreen is dispatched THEN it navigates to register`() = runTest {
        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.GoToCreateAccountScreen)

        assertEquals(1, effects.size)
        assertEquals(LoginEffect.NavigateToRegisterAccount, effects.first())
        job.cancel()
    }

    @Test
    fun `WHEN GoToConfigurationScreen is dispatched THEN it navigates to configuration`() = runTest {
        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.GoToConfigurationScreen)

        assertEquals(1, effects.size)
        assertEquals(LoginEffect.NavigateToConfiguration, effects.first())
        job.cancel()
    }

    @Test
    fun `WHEN GoToBleDiscovery is dispatched THEN it navigates to ble discovery`() = runTest {
        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.GoToBleDiscovery)

        assertEquals(1, effects.size)
        assertEquals(LoginEffect.NavigateToBleDiscovery, effects.first())
        job.cancel()
    }

    @Test
    fun `WHEN Login succeeds with an account THEN it activates the server session and starts device listeners`() = runTest {
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())
        coEvery { loginUseCase(any(), any()) } returns Outcome.Ok(Unit)
        coEvery { localDataRepository.getCurrentAccount() } returns account
        coEvery { startDeviceListenersUseCase(account.devices) } returns Outcome.Ok(Unit)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        coVerify(exactly = 1) { appSessionRepository.activateServerSession(account) }
        coVerify(exactly = 1) { startDeviceListenersUseCase(account.devices) }
        assertEquals(LoginEffect.NavigateHome, effects.last())
        job.cancel()
    }

    @Test
    fun `WHEN Login succeeds but starting listeners returns errors THEN it emits ConnectionErrors before navigating home`() = runTest {
        val account = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())
        val errors = listOf(ConnectionError.Unknown)
        coEvery { loginUseCase(any(), any()) } returns Outcome.Ok(Unit)
        coEvery { localDataRepository.getCurrentAccount() } returns account
        coEvery { startDeviceListenersUseCase(account.devices) } returns Outcome.Err(errors)

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertEquals(2, effects.size)
        assertEquals(LoginEffect.ConnectionErrors(errors), effects[0])
        assertEquals(LoginEffect.NavigateHome, effects[1])
        job.cancel()
    }

    @Test
    fun `WHEN Login times out THEN it shows a could not connect snackbar`() = runTest {
        coEvery { loginUseCase(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(60_000L)
            Outcome.Ok(Unit)
        }

        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.Login)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1, effects.size)
        val effect = effects.first()
        assertTrue(effect is LoginEffect.ShowSnackbar)
        assertEquals("Could not connect to server", (effect as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }
}
