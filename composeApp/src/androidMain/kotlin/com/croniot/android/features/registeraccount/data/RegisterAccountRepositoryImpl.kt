package com.croniot.android.features.registeraccount.data

import com.croniot.client.domain.repositories.RegisterAccountRepository
import com.croniot.client.core.config.Constants.ENDPOINT_REGISTER_ACCOUNT
import com.croniot.client.data.source.remote.http.NetworkUtil
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import java.util.UUID

class RegisterAccountRepositoryImpl(
    private val networkUtil: NetworkUtil,
) : RegisterAccountRepository {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override suspend fun registerAccount(nickname: String, email: String, password: String): Result {
        val message = MessageRegisterAccount(UUID.randomUUID().toString(), nickname, email, password)
        return networkUtil.post(ENDPOINT_REGISTER_ACCOUNT, gson.toJson(message))
    }
}
