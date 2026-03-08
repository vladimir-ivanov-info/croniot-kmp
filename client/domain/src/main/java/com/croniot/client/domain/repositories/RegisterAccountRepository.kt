package com.croniot.client.domain.repositories

import croniot.models.Result

interface RegisterAccountRepository {
    suspend fun registerAccount(nickname: String, email: String, password: String): Result
}
