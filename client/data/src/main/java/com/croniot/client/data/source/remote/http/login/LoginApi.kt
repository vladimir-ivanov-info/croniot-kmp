package com.croniot.client.data.source.remote.http.login

import croniot.messages.MessageLoginRequest
import croniot.models.LoginResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApi {
    @POST("/api/login")
    suspend fun login(@Body request: MessageLoginRequest): Response<LoginResultDto>
}