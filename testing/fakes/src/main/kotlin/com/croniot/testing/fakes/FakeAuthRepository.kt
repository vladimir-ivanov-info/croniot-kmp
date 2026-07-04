package com.croniot.testing.fakes

import Outcome
import com.croniot.client.domain.LoginResult
import com.croniot.client.domain.models.auth.AuthError
import com.croniot.client.domain.repositories.AuthRepository

class FakeAuthRepository(
    private var loginOutcome: Outcome<LoginResult, AuthError> = Outcome.Err(AuthError.Unknown),
) : AuthRepository {

    data class LoginInvocation(
        val email: String,
        val password: String,
        val deviceUuid: String,
        val deviceToken: String?,
        val deviceProperties: Map<String, String>,
    )

    val loginInvocations: MutableList<LoginInvocation> = mutableListOf()

    fun configureLoginResponse(outcome: Outcome<LoginResult, AuthError>) {
        loginOutcome = outcome
    }

    override suspend fun login(
        email: String,
        password: String,
        deviceUuid: String,
        deviceToken: String?,
        deviceProperties: Map<String, String>,
    ): Outcome<LoginResult, AuthError> {
        loginInvocations += LoginInvocation(email, password, deviceUuid, deviceToken, deviceProperties)
        return loginOutcome
    }
}
