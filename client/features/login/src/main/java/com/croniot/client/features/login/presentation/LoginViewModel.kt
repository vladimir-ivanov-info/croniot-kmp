package com.croniot.client.features.login.presentation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.croniot.client.core.config.AppConfig
import com.croniot.client.core.config.Constants.DEMO_EMAIL
import com.croniot.client.domain.models.auth.AuthError
// import com.croniot.client.domain.models.auth.Outcome
// import com.croniot.client.data.strategy.DataSourceStrategy
// import com.croniot.client.data.strategy.DataSourceStrategyBus
import com.croniot.client.domain.repositories.AppSessionRepository
import com.croniot.client.domain.repositories.LocalDataRepository
import com.croniot.client.domain.usecases.LogInUseCase
import com.croniot.client.domain.usecases.StartDeviceListenersUseCase
import com.croniot.client.presentation.viewmodel.launchInVmScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent

class LoginViewModel(
    private val loginUseCase: LogInUseCase,
    private val localDataRepository: LocalDataRepository,
    private val startDeviceListenersUseCase: StartDeviceListenersUseCase,
    private val appSessionRepository: AppSessionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), KoinComponent {

    companion object {
        private const val KEY_LOGIN_STATE = "login_state"
        private const val LOGIN_TIMEOUT_MILLIS = 99995_000L
        // const val DEMO_EMAIL = "croniot_demo@email.com"
    }

    private val _state = MutableStateFlow(
        savedStateHandle.get<LoginState>(KEY_LOGIN_STATE) ?: LoginState(),
    )
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>(replay = 0, extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private inline fun updateState(transform: (LoginState) -> LoginState) {
        _state.update { current ->
            val newState = transform(current)
            // TODO for production: savedStateHandle[KEY_STATE] = newState.copy(password = "") //TODO turn into build variant
            savedStateHandle[KEY_LOGIN_STATE] = newState
            newState
        }
    }

    fun onAction(action: LoginIntent) {
        when (action) {
            is LoginIntent.EmailChanged -> updateState { it.copy(email = action.value) }
            is LoginIntent.PasswordChanged -> updateState { it.copy(password = action.value) }
            LoginIntent.Login -> login()
            LoginIntent.LoginAsGuest -> loginAsGuest()
            LoginIntent.GoToCreateAccountScreen -> sendEffect(LoginEffect.NavigateToRegisterAccount)
            LoginIntent.GoToConfigurationScreen -> sendEffect(LoginEffect.NavigateToConfiguration)
            LoginIntent.GoToBleDiscovery -> sendEffect(LoginEffect.NavigateToBleDiscovery)
        }
    }

    private fun sendEffect(effect: LoginEffect) {
        _effects.tryEmit(effect)
    }

    private fun login() = launchInVmScope {
        updateState {
            it.copy(isLoading = true)
        }

        val email = state.value.email

        /*if (email == DEMO_EMAIL) {
            dataSourceBus.setDataSourceStrategy(DataSourceStrategy.DEMO)
        } else {
            dataSourceBus.setDataSourceStrategy(DataSourceStrategy.REAL)
        }*/

        withTimeoutOrNull(LOGIN_TIMEOUT_MILLIS) {
            when (val result = loginUseCase(state.value.email, state.value.password)) {
                is Outcome.Ok -> {
                    _state.update { it.copy(isLoading = false) }
                    localDataRepository.getCurrentAccount()?.let { account ->
                        appSessionRepository.activateServerSession(account)
                        val listenersResult = startDeviceListenersUseCase(account.devices)
                        if (listenersResult is Outcome.Err) {
                            sendEffect(LoginEffect.ConnectionErrors(listenersResult.error))
                        }
                    }
                    sendEffect(LoginEffect.NavigateHome)
                }
                is Outcome.Err -> {
                    _state.update { it.copy(isLoading = false) }
                    sendEffect(
                        LoginEffect.ShowSnackbar(
                            title = "Login failed",
                            content = result.error.toUserMessage(),
                        ),
                    )
                }
            }
        } ?: run {
            updateState {
                it.copy(isLoading = false)
            }

            sendEffect(
                LoginEffect.ShowSnackbar(
                    title = "Login failed",
                    content = "Could not connect to server",
                ),
            )
        }
        updateState {
            it.copy(isLoading = false)
        }
    }

    private fun loginAsGuest() {
        // change datasources
    }
}

private fun AuthError.toUserMessage(): String = when (this) {
    AuthError.Network -> "No hay conexión con el servidor."
    AuthError.NetworkTiemout -> "Timeout con el servidor."
    AuthError.InvalidCredentials -> "Credenciales inválidas."
    AuthError.DeviceMissing -> "No se encontró el identificador del dispositivo."
    is AuthError.Server -> message ?: "Error de servidor."
    AuthError.Unknown -> "Error desconocido."
    AuthError.AccountMissing -> "No account returned"
    AuthError.TokenMissing -> "No token returned"
}

@Parcelize
data class LoginState(
    val email: String = /*if (AppConfig.isDemo) DEMO_EMAIL else*/ "email1@gmail.com",
    // val email: String = LoginViewModel.DEMO_EMAIL,
    val password: String = "password1",
    val isLoading: Boolean = false,
) : Parcelable

sealed interface LoginEffect {
    data object NavigateHome : LoginEffect
    data object NavigateToRegisterAccount : LoginEffect
    data object NavigateToConfiguration : LoginEffect
    data object NavigateToBleDiscovery : LoginEffect
    data class ShowSnackbar(val title: String, val content: String) : LoginEffect
    data class ConnectionErrors(val errors: List<com.croniot.client.domain.models.ConnectionError>) : LoginEffect
}

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
    data object Login : LoginIntent
    data object LoginAsGuest : LoginIntent
    data object GoToCreateAccountScreen : LoginIntent
    data object GoToConfigurationScreen : LoginIntent
    data object GoToBleDiscovery : LoginIntent
}
