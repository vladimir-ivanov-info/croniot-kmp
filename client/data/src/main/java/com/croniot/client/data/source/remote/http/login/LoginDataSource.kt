package com.croniot.client.data.source.remote.http.login

import Outcome
import com.croniot.client.domain.models.auth.AuthError
import croniot.messages.LoginDto
import croniot.models.LoginResultDto

interface LoginDataSource {
    suspend fun login(request: LoginDto): Outcome<LoginResultDto, AuthError>
}
