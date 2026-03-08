package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.core.models.auth.AuthError
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.mappers.toDomain
import com.croniot.client.domain.repositories.AuthRepository
import com.croniot.client.domain.LoginResult
import croniot.messages.LoginDto
import croniot.models.LoginResultDto

class AuthRepositoryImpl(
    private val loginDataSource: LoginDataSource,
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        deviceUuid: String,
        deviceToken: String?,
        deviceProperties: Map<String, String>,
    ): Outcome<LoginResult, AuthError> {
        val loginRequest = LoginDto(
            email = email,
            password = password,
            deviceUuid = deviceUuid,
            deviceToken = deviceToken,
            deviceProperties = deviceProperties,
        )

        return when (val result = loginDataSource.login(loginRequest)) {
            is Outcome.Err -> result
            is Outcome.Ok -> mapToLoginResult(result.value)
        }
    }

    private fun mapToLoginResult(body: LoginResultDto): Outcome<LoginResult, AuthError> {
        val account = body.accountDto?.toDomain()
        val token = body.token
        return when {
            account != null && token != null -> Outcome.Ok(LoginResult(account = account, token = token))
            !body.result.success -> Outcome.Err(AuthError.InvalidCredentials)
            account == null -> Outcome.Err(AuthError.AccountMissing)
            else -> Outcome.Err(AuthError.TokenMissing)
        }
    }
}
