package com.croniot.client.data.source.remote.http

import com.croniot.client.core.config.Constants
import croniot.messages.MessageRegisterAccount
import croniot.models.Result
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class RegisterApi(private val http: HttpClient) {

    suspend fun registerAccount(message: MessageRegisterAccount): Result =
        http.post(Constants.ENDPOINT_REGISTER_ACCOUNT) {
            contentType(ContentType.Application.Json)
            setBody(message)
        }.body()
}
