package com.croniot.client.data.repositories

import com.croniot.client.core.models.auth.AuthError
import com.croniot.client.core.models.auth.Outcome
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.http.login.LoginResultDomain
import com.croniot.client.data.source.remote.mappers.toDomain
import croniot.messages.MessageLoginRequest

class AuthRepositoryImpl(
    private val loginDataSource: LoginDataSource,
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        deviceUuid: String,
        deviceToken: String?,
        deviceProperties: Map<String, String>,
    ): Outcome<LoginResultDomain, AuthError> {
        var result: Outcome<LoginResultDomain, AuthError> = Outcome.Err(AuthError.Unknown)

        try {
            val loginRequest = MessageLoginRequest(
                email = email,
                password = password,
                deviceUuid = deviceUuid,
                deviceToken = deviceToken,
                deviceProperties = deviceProperties,
            )

            val loginResponse = loginDataSource.login(loginRequest)

            /*if (!loginResponse.isSuccessful) {
                Outcome.Err(AuthError.Server(loginResponse.errorBody()?.string()))
            }*/

            if (loginResponse.isFailure) {
                Outcome.Err(AuthError.Server(loginResponse.getOrNull()?.result?.message))
            }

            val body = loginResponse.getOrNull() // body()

            if (body == null) {
                Outcome.Err(AuthError.Unknown)
            } else {
                // if (!loginResponse.isSuccessful ) {
                // val responseMessage = loginResponse.message()
                val responseMessage = body.toString() // TODO test and check the body

                val account = body.account
                val token = body.token

                if (account != null && token != null) {
                    result = Outcome.Ok(
                        LoginResultDomain(
                            account = account.toDomain(),
                            token = token,
                        ),
                    )
                } else {
                    result = when {
                        responseMessage.contains("invalid", ignoreCase = true) -> Outcome.Err(AuthError.InvalidCredentials)
                        account == null -> Outcome.Err(AuthError.AccountMissing)
                        token == null -> Outcome.Err(AuthError.TokenMissing)
                        else -> Outcome.Err(AuthError.Unknown)
                    }
                }
            }
        } catch (e: Exception) {
            result = Outcome.Err(AuthError.Network)
        }
        return result
    }
}
