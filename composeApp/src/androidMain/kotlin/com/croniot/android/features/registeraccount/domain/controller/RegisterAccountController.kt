package com.croniot.android.features.registeraccount.domain.controller

import com.croniot.android.core.data.source.local.SharedPreferences
import com.croniot.android.features.registeraccount.domain.usecase.RegisterAccountUseCase
import croniot.models.Result
import java.util.UUID

class RegisterAccountController(private val registerAccountUseCase: RegisterAccountUseCase) {

    suspend fun registerAccount(nickname: String, email: String, password: String): Result {
        val accountUuid = UUID.randomUUID().toString() // Generate a unique UUID

        val result = registerAccountUseCase(accountUuid, nickname, email, password)
        if (result.success) {
            SharedPreferences.saveData(SharedPreferences.KEY_ACCOUNT_EMAIL, email)
            SharedPreferences.saveData(SharedPreferences.KEY_ACCOUNT_PASSWORD, password)
        }
        return  result
    }
}