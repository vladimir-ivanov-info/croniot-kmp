package com.croniot.android.features.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.core.data.source.repository.AccountRepository
import com.croniot.android.features.login.usecase.LoginUseCase
import com.croniot.android.features.login.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val accountRepository: AccountRepository,
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow(LoginUiState(email = "email1@gmail.com", password = "password1")) // TODO this is a demo account
    val uiState: StateFlow<LoginUiState> get() = _uiState

    val email: StateFlow<String> get() = _uiState.map { it.email }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.email)

    val password: StateFlow<String> get() = _uiState.map { it.password }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.password)

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() { // TODO move to LoginUseCase
        viewModelScope.launch {
            val email = _uiState.value.email
            val password = _uiState.value.password

            val result = loginUseCase(email, password) // TODO delegate to controller

            _uiState.value = _uiState.value.copy(password = "")

            if (result.result.success) {
                _uiState.value = _uiState.value.copy(isLoading = false, loggedIn = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.result.message,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            accountRepository.clearAccount() // TODO move inside of logoutUseCase
            _uiState.value = LoginUiState() // Reset UI state
        }
    }
}
