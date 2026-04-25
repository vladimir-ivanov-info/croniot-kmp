package com.croniot.client.data.repositories

import Outcome
import com.croniot.client.domain.models.auth.AuthError
import com.croniot.client.domain.models.auth.AuthTokens
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.mappers.toDomain
import com.croniot.client.domain.LoginResult
import com.croniot.client.domain.repositories.AuthRepository
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
        val accessToken = body.token
        val refreshToken = body.refreshToken
        val expiresAt = body.accessTokenExpiresAtEpochSeconds
        return when {
            account != null && accessToken != null && refreshToken != null && expiresAt != null ->
                Outcome.Ok(
                    LoginResult(
                        account = account,
                        tokens = AuthTokens(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresAtEpochSeconds = expiresAt,
                        ),
                    ),
                )
            !body.result.success -> Outcome.Err(AuthError.InvalidCredentials)
            account == null -> Outcome.Err(AuthError.AccountMissing)
            else -> Outcome.Err(AuthError.TokenMissing)
        }
    }
}
