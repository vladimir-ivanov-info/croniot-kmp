package com.croniot.client.data.repositories

import com.croniot.client.core.config.Constants.ENDPOINT_REGISTER_ACCOUNT
import com.croniot.client.data.source.remote.http.NetworkUtil
import com.croniot.client.domain.repositories.RegisterAccountRepository
import croniot.messages.MessageFactory
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import java.util.UUID

class RegisterAccountRepositoryImpl(
    private val networkUtil: NetworkUtil,
) : RegisterAccountRepository {

    override suspend fun registerAccount(nickname: String, email: String, password: String): Result {
        val message = MessageRegisterAccount(UUID.randomUUID().toString(), nickname, email, password)
        return networkUtil.post(ENDPOINT_REGISTER_ACCOUNT, MessageFactory.toJson(message))
    }
}