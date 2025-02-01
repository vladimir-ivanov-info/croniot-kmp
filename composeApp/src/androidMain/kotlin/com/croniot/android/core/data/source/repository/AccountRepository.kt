package com.croniot.android.core.data.source.repository

import com.croniot.android.core.data.source.local.DataStoreController
import croniot.models.dto.AccountDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class AccountRepository : KoinComponent {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _account = MutableStateFlow<AccountDto?>(null)
    val account: StateFlow<AccountDto?> get() = _account

    init {
        repositoryScope.launch {
            DataStoreController.getAccount().collect { account ->
                _account.value = account
            }
        }
    }

    fun updateAccount(newAccount: AccountDto) {
        repositoryScope.launch {
            DataStoreController.saveAccount(newAccount)
        }
    }

    fun clearAccount() {
        repositoryScope.launch {
            DataStoreController.saveAccount(null)
        }
    }
}
