package com.croniot.client.data.source.remote.http.login

import com.croniot.client.core.config.Constants
import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class LoginApi(private val http: HttpClient) {

    suspend fun login(request: LoginDto): LoginResultDto =
        http.post(Constants.ENDPOINT_LOGIN) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
