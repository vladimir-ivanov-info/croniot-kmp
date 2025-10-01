package com.croniot.client.data.source.remote.http.login

import croniot.messages.MessageLoginRequest
import croniot.models.LoginResultDto
import retrofit2.HttpException

class LoginDataSourceImpl(
    private val api: LoginApi,
) : LoginDataSource {
    override suspend fun login(request: MessageLoginRequest): Result<LoginResultDto> = runCatching {
        val resp = api.login(request)
        if (!resp.isSuccessful) {
            throw HttpException(resp) // o crea tu propio error
        }
        val dto = resp.body() ?: error("Empty body")
        dto // .toDomain() // mapea DTO → LoginResult
    }
}
