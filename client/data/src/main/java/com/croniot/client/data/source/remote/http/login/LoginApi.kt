package com.croniot.client.data.source.remote.http.login

import com.croniot.client.core.config.Constants
import croniot.messages.LoginDto
import croniot.models.LoginResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApi {
    @POST(Constants.ENDPOINT_LOGIN)
    suspend fun login(@Body request: LoginDto): Response<LoginResultDto>
}
