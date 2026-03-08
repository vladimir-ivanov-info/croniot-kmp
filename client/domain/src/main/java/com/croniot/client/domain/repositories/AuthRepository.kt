package com.croniot.client.domain.repositories

import Outcome
import com.croniot.client.core.models.auth.AuthError
import com.croniot.client.domain.LoginResult

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String,
        deviceUuid: String,
        deviceToken: String?,
        deviceProperties: Map<String, String>,
    ): Outcome<LoginResult, AuthError>
}
