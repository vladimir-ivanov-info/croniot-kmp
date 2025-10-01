package com.croniot.android.features.registeraccount.presentation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.features.registeraccount.domain.usecase.RegisterAccountUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize

class ViewModelRegisterAccount(
    private val registerAccountUseCase: RegisterAccountUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val KEY_CREATE_ACCOUNT_STATE = "create_account_state"
        private const val CREATE_ACCOUNT_TIMEOUT_MILLIS = 9995_000L
    }

    private val _state = MutableStateFlow(
        savedStateHandle.get<CreateAccountState>(KEY_CREATE_ACCOUNT_STATE) ?: CreateAccountState(),
    )
    val state: StateFlow<CreateAccountState> = _state.asStateFlow()

    private val _effects = Channel<CreateAccountEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<CreateAccountEffect> = _effects.receiveAsFlow()

    private inline fun updateState(transform: (CreateAccountState) -> CreateAccountState) {
        _state.update { current ->
            val newState = transform(current)
            // TODO for production: savedStateHandle[KEY_STATE] = newState.copy(password = "")
            savedStateHandle[KEY_CREATE_ACCOUNT_STATE] = newState
            newState
        }
    }

    fun onAction(action: RegisterAccountIntent) {
        when (action) {
            is RegisterAccountIntent.NicknameChanged -> updateState { it.copy(nickname = action.value) }
            is RegisterAccountIntent.EmailChanged -> updateState { it.copy(email = action.value) }
            is RegisterAccountIntent.PasswordChanged -> updateState { it.copy(password = action.value) }
            is RegisterAccountIntent.RegisterAccount -> registerAccount()
            is RegisterAccountIntent.NavigateBack -> sendEffect(CreateAccountEffect.NavigateBack)
        }
    }

    private fun sendEffect(effect: CreateAccountEffect) {
        viewModelScope.launch {
            _effects.send(effect)
        }
    }

    private fun registerAccount() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            // TODO check all fields correct (not empty, etc) before registering
            withTimeoutOrNull(CREATE_ACCOUNT_TIMEOUT_MILLIS) {
                val result = registerAccountUseCase.invoke(
                    nickname = state.value.nickname,
                    email = state.value.email,
                    password = state.value.password,
                )

                // For testing: val result  = Result(true, "good")
                if (result.success) {
                    updateState {
                        it.copy(isLoading = false)
                    }
                    sendEffect(
                        CreateAccountEffect.ShowSnackbar(
                            title = "Create account",
                            content = "Account created successfully",
                        ),
                    )
                } else {
                    updateState {
                        it.copy(isLoading = false)
                    }

                    sendEffect(
                        CreateAccountEffect.ShowSnackbar(
                            title = "Create account failed",
                            content = result.message,
                        ),
                    )
                }
            } ?: run {
                updateState {
                    it.copy(isLoading = false)
                }

                sendEffect(
                    CreateAccountEffect.ShowSnackbar(
                        title = "Create account failed",
                        content = "Could not connect to server",
                    ),
                )
            }
        }
    }
}

@Parcelize
data class CreateAccountState(
    val nickname: String = "vladimir",
    val email: String = "email1@gmail.com",
    val password: String = "password1",
    val isLoading: Boolean = false,
) : Parcelable

sealed interface CreateAccountEffect {
    data object NavigateBack : CreateAccountEffect
    data class ShowSnackbar(val title: String, val content: String) : CreateAccountEffect
}

sealed interface RegisterAccountIntent {
    data class NicknameChanged(val value: String) : RegisterAccountIntent
    data class EmailChanged(val value: String) : RegisterAccountIntent
    data class PasswordChanged(val value: String) : RegisterAccountIntent
    data object RegisterAccount : RegisterAccountIntent
    data object NavigateBack : RegisterAccountIntent
}
