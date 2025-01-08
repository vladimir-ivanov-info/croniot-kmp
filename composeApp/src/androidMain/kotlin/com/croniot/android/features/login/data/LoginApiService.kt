package com.croniot.android.features.login.data

import croniot.messages.MessageLogin
import croniot.models.LoginResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApiService {

    @POST("/api/login")
    suspend fun login(@Body loginRequest: MessageLogin): Response<LoginResult>
}