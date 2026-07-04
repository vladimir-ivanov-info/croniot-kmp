package com.croniot.client.data.repositories

import com.croniot.client.data.source.remote.http.RegisterApi
import com.croniot.client.domain.repositories.RegisterAccountRepository
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class RegisterAccountRepositoryImpl(
    private val registerApi: RegisterApi,
) : RegisterAccountRepository {

    override suspend fun registerAccount(nickname: String, email: String, password: String): Result {
        val message = MessageRegisterAccount(UUID.randomUUID().toString(), nickname, email, password)
        return try {
            registerApi.registerAccount(message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result(false, e.message ?: "Network error")
        } catch (e: Exception) {
            Result(false, e.message ?: "Unknown error")
        }
    }
}
