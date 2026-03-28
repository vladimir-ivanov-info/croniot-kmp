package com.croniot.client.data.source.remote.http

import com.croniot.client.core.config.Constants
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface TaskConfigurationApiService {
    @Headers("Accept: application/json")
    @GET(Constants.ENDPOINT_TASK_CONFIGURATION)
    suspend fun requestTaskConfigurations(@Path("deviceUuid") deviceUuid: String): Response<List<TaskDto>>

    @Headers("Accept: application/json")
    @GET(Constants.ENDPOINT_TASK_STATE_INFO_HISTORY)
    suspend fun requestTaskStateInfoHistory(@Path("deviceUuid") deviceUuid: String): Response<List<TaskStateInfoHistoryEntryDto>>
}
