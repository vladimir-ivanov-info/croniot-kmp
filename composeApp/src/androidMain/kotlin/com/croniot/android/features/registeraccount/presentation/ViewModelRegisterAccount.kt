package com.croniot.android.features.registeraccount.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.croniot.android.features.registeraccount.domain.controller.RegisterAccountController
import croniot.models.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ViewModelRegisterAccount(private val registerAccountController: RegisterAccountController) : ViewModel() {

    private val _nickname = MutableStateFlow("vladimiriot") // default for testing
    val nickname: StateFlow<String> get() = _nickname

    private val _email = MutableStateFlow("email1@gmail.com") // default for testing
    val email: StateFlow<String> get() = _email

    private val _password = MutableStateFlow("password1") // default for testing
    val password: StateFlow<String> get() = _password

    fun updateNickname(nickname: String) {
        viewModelScope.launch { _nickname.emit(nickname) }
    }

    fun updateEmail(email: String) {
        viewModelScope.launch { _email.emit(email) }
    }

    fun updatePassword(password: String) {
        viewModelScope.launch { _password.emit(password) }
    }

    fun registerAccount(onResult: (Result) -> Unit) {
        viewModelScope.launch {
            val result = registerAccountController.registerAccount(
                nickname.value,
                email.value,
                password.value
            )
            onResult(result)
        }
    }
}
