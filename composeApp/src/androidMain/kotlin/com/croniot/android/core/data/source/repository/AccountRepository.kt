package com.croniot.android.core.data.source.repository

import com.croniot.android.core.data.source.local.SharedPreferences
import croniot.models.dto.AccountDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent

class AccountRepository : KoinComponent {

    private val _account = MutableStateFlow<AccountDto?>(null)
    val account: StateFlow<AccountDto?> get() = _account

    init {
        _account.value = SharedPreferences.getAccout()
    }

    fun updateAccount(newAccount: AccountDto) {
        _account.value = newAccount
        SharedPreferences.saveAccount(newAccount)
    }

    fun clearAccount() {
        _account.value = null
    }
}
