package com.croniot.android.features.registeraccount.domain.controller

import com.croniot.android.core.data.source.local.DataStoreController
import com.croniot.android.features.registeraccount.domain.usecase.RegisterAccountUseCase
import croniot.models.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class RegisterAccountController(private val registerAccountUseCase: RegisterAccountUseCase) {

    suspend fun registerAccount(nickname: String, email: String, password: String): Result {
        val accountUuid = UUID.randomUUID().toString()

        val result = registerAccountUseCase(accountUuid, nickname, email, password)
        if (result.success) {
            CoroutineScope(Dispatchers.IO).launch {
                DataStoreController.saveData(DataStoreController.KEY_ACCOUNT_EMAIL, email)
                DataStoreController.saveData(DataStoreController.KEY_ACCOUNT_PASSWORD, password)
            }
        }
        return result
    }
}
