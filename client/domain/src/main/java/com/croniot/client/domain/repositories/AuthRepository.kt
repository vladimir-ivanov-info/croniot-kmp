package com.croniot.client.domain.repositories

import Outcome
import com.croniot.client.domain.LoginResult
import com.croniot.client.domain.models.auth.AuthError

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String,
        deviceUuid: String,
        deviceToken: String?,
        deviceProperties: Map<String, String>,
    ): Outcome<LoginResult, AuthError>
}
