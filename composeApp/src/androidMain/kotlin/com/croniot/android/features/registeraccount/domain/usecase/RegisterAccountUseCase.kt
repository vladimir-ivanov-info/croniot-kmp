package com.croniot.android.features.registeraccount.domain.usecase

import com.croniot.android.core.presentation.util.NetworkUtil
import croniot.models.Result
import croniot.messages.MessageRegisterAccount
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RegisterAccountUseCase {

    suspend operator fun invoke(accountUuid: String, nickname: String, email: String, password: String): Result {
        val message = MessageRegisterAccount(accountUuid, nickname, email, password)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val messageJson = gson.toJson(message)

        return withContext(Dispatchers.IO){
            NetworkUtil.performPostRequestToEndpoint("/api/register_account", messageJson)
        }
    }
}