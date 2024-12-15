package com.croniot.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import croniot.models.dto.AccountDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class GlobalViewModel : ViewModel(), KoinComponent {

    private var _account : MutableStateFlow<AccountDto?> = MutableStateFlow(null)
    val account : MutableStateFlow<AccountDto?> get() = _account

    init {
        val account = SharedPreferences.getAccout()
        viewModelScope.launch {
            account?.let{
                _account.emit(account)
            }
        }
    }

    fun updateAccount(newAccount: AccountDto) {
        SharedPreferences.saveAccount(newAccount)
        viewModelScope.launch {
            _account.emit(newAccount)
        }
    }

}
