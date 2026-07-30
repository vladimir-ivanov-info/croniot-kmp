package com.croniot.android.features.registeraccount.presentation

import androidx.lifecycle.SavedStateHandle
import com.croniot.client.domain.usecases.RegisterAccountUseCase
import croniot.models.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class ViewModelRegisterAccountTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val registerAccountUseCase: RegisterAccountUseCase = mockk()
    private lateinit var viewModel: ViewModelRegisterAccount

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ViewModelRegisterAccount(
            registerAccountUseCase = registerAccountUseCase,
            savedStateHandle = SavedStateHandle(),
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Given nickname email and password changed, When onAction, Then updates each field in state`() = runTest {
        viewModel.onAction(RegisterAccountIntent.NicknameChanged("newNickname"))
        viewModel.onAction(RegisterAccountIntent.EmailChanged("new@email.com"))
        viewModel.onAction(RegisterAccountIntent.PasswordChanged("newPassword"))

        assertEquals("newNickname", viewModel.state.value.nickname)
        assertEquals("new@email.com", viewModel.state.value.email)
        assertEquals("newPassword", viewModel.state.value.password)
    }

    @Test
    fun `Given registerAccountUseCase succeeds, When RegisterAccount, Then isLoading is false and shows success snackbar`() =
        runTest {
            coEvery {
                registerAccountUseCase(nickname = any(), email = any(), password = any())
            } returns Result(success = true, message = "ok")

            viewModel.onAction(RegisterAccountIntent.RegisterAccount)
            val effect = viewModel.effects.first()

            assertFalse(viewModel.state.value.isLoading)
            assertTrue(effect is CreateAccountEffect.ShowSnackbar)
        }

    @Test
    fun `Given registerAccountUseCase fails, When RegisterAccount, Then isLoading is false and shows error snackbar`() =
        runTest {
            coEvery {
                registerAccountUseCase(nickname = any(), email = any(), password = any())
            } returns Result(success = false, message = "boom")

            viewModel.onAction(RegisterAccountIntent.RegisterAccount)
            val effect = viewModel.effects.first() as CreateAccountEffect.ShowSnackbar

            assertFalse(viewModel.state.value.isLoading)
            assertEquals("boom", effect.content)
        }

    @Test
    fun `Given NavigateBack action, When onAction, Then emits NavigateBack effect`() = runTest {
        viewModel.onAction(RegisterAccountIntent.NavigateBack)
        val effect = viewModel.effects.first()

        assertEquals(CreateAccountEffect.NavigateBack, effect)
    }

    @Test
    fun `Given registerAccountUseCase never returns, When RegisterAccount, Then shows could not connect snackbar`() =
        runTest {
            coEvery {
                registerAccountUseCase(nickname = any(), email = any(), password = any())
            } coAnswers {
                kotlinx.coroutines.delay(10_000_000L)
                Result(success = true)
            }

            viewModel.onAction(RegisterAccountIntent.RegisterAccount)
            advanceUntilIdle()
            val effect = viewModel.effects.first() as CreateAccountEffect.ShowSnackbar

            assertFalse(viewModel.state.value.isLoading)
            assertEquals("Could not connect to server", effect.content)
        }
}
