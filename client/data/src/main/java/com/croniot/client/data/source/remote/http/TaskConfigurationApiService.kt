package com.croniot.client.data.source.remote.http

import com.croniot.client.core.config.Constants
import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskConfigurationApiService {
    @Headers("Accept: application/json")
    @GET(Constants.ENDPOINT_TASK_CONFIGURATION)
    suspend fun requestTaskConfigurations(@Path("deviceUuid") deviceUuid: String): Response<List<TaskDto>>

    @Headers("Accept: application/json")
    @GET(Constants.ENDPOINT_TASK_STATE_INFO_HISTORY)
    suspend fun requestTaskStateInfoHistory(
        @Path("deviceUuid") deviceUuid: String,
        @Query("limit") limit: Int,
        @Query("before") before: String? = null,
        @Query("beforeId") beforeId: Long? = null,
        @Query("taskTypeUids") taskTypeUids: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
    ): Response<List<TaskStateInfoHistoryEntryDto>>

    @Headers("Accept: application/json")
    @GET(Constants.ENDPOINT_TASK_STATE_INFO_HISTORY_COUNT)
    suspend fun requestTaskStateInfoHistoryCount(
        @Path("deviceUuid") deviceUuid: String,
        @Query("before") before: String? = null,
        @Query("beforeId") beforeId: Long? = null,
        @Query("taskTypeUids") taskTypeUids: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
    ): Response<Int>
}
