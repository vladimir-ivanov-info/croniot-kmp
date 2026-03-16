package com.croniot.client.features.login.presentation

import Outcome
import androidx.lifecycle.SavedStateHandle
import com.croniot.client.core.models.auth.AuthError
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.LogInUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import io.mockk.coEvery
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
    private lateinit var viewModel: LoginViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loginUseCase = mockk()
        localDataRepository = mockk()
        startDeviceListenersUseCase = mockk()

        viewModel = LoginViewModel(
            loginUseCase = loginUseCase,
            localDataRepository = localDataRepository,
            startDeviceListenersUseCase = startDeviceListenersUseCase,
            savedStateHandle = SavedStateHandle(),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `on EmailChanged action, state is updated`() = runTest {
        val newEmail = "test@example.com"
        viewModel.onAction(LoginIntent.EmailChanged(newEmail))

        assertEquals(newEmail, viewModel.state.value.email)
    }

    @Test
    fun `on PasswordChanged action, state is updated`() = runTest {
        val newPassword = "new_password"
        viewModel.onAction(LoginIntent.PasswordChanged(newPassword))

        assertEquals(newPassword, viewModel.state.value.password)
    }

    @Test
    fun `on Login success, navigates home`() = runTest {
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
    fun `on Login failure, shows snackbar with correct message`() = runTest {
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
        assertEquals("Credenciales inválidas.", (effect as LoginEffect.ShowSnackbar).content)
        job.cancel()
    }

    @Test
    fun `on GoToCreateAccountScreen action, navigates to register`() = runTest {
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
    fun `on GoToConfigurationScreen action, navigates to configuration`() = runTest {
        val effects = mutableListOf<LoginEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.onAction(LoginIntent.GoToConfigurationScreen)

        assertEquals(1, effects.size)
        assertEquals(LoginEffect.NavigateToConfiguration, effects.first())
        job.cancel()
    }
}
