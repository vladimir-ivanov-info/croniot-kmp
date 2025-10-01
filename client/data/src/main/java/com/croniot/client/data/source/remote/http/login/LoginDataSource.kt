package com.croniot.client.data.source.remote.http.login

import croniot.messages.MessageLoginRequest
import croniot.models.LoginResultDto

interface LoginDataSource {
    suspend fun login(request: MessageLoginRequest): Result<LoginResultDto>
}
