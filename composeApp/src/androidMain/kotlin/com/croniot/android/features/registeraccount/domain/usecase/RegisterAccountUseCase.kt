package com.croniot.android.features.registeraccount.domain.usecase

import com.croniot.client.core.Constants.ENDPOINT_REGISTER_ACCOUNT
import com.croniot.client.data.source.remote.http.NetworkUtilImpl
import com.google.gson.GsonBuilder
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import java.util.UUID

class RegisterAccountUseCase(
    private val networkUtilImpl: NetworkUtilImpl
) {

    suspend operator fun invoke(nickname: String, email: String, password: String): Result {
        val accountUuid = UUID.randomUUID().toString()
        val message = MessageRegisterAccount(accountUuid, nickname, email, password)
        val gson = GsonBuilder().setPrettyPrinting().create()
        val messageJson = gson.toJson(message)

       /* return withContext(Dispatchers.IO) {
            NetworkUtil.performPostRequestToEndpoint("/api/register_account", messageJson)
        }*/
        return networkUtilImpl.post(ENDPOINT_REGISTER_ACCOUNT, messageJson)
    }
}
