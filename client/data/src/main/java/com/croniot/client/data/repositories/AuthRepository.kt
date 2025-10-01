package com.croniot.client.data.repositories

import com.croniot.client.core.models.auth.AuthError
import com.croniot.client.core.models.auth.Outcome
import com.croniot.client.data.source.remote.http.login.LoginResultDomain

// import com.croniot.client.data.source.remote.http.login.LoginResultDomain

interface AuthRepository {

    suspend fun login(
        email: String,
        password: String,
        deviceUuid: String,
        deviceToken: String?,
        deviceProperties: Map<String, String>,
    ): Outcome<LoginResultDomain, AuthError>
}
