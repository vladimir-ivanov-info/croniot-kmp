package com.croniot.client.data.source.remote.http

import croniot.models.dto.TaskDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface TaskConfigurationApiService {
    @Headers("Accept: application/json")
    @GET("/taskConfiguration/{deviceUuid}")
    suspend fun requestTaskConfigurations(@Path("deviceUuid") deviceUuid: String): Response<List<TaskDto>>
}
