package com.croniot.android.core.data.source.repository

import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.domain.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class AccountRepository : KoinComponent {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> get() = _account

    init {
        repositoryScope.launch {
            DataStoreController.getAccount().collect { account ->
                _account.value = account
            }
        }
    }

    fun updateAccount(newAccount: Account) {
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
