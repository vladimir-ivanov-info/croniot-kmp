package com.croniot.client.features.tasktypes.usecases

import com.croniot.client.core.Constants.ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC
import com.croniot.client.data.source.remote.http.NetworkUtilImpl
import com.google.gson.GsonBuilder
import croniot.messages.MessageRequestTaskStateInfoSync
import croniot.models.Result

class RequestTaskStateInfoSyncUseCase(
    private val networkUtilImpl: NetworkUtilImpl
) {

    suspend operator fun invoke(deviceUuid: String, taskUid: Long) : Result {
        val message = MessageRequestTaskStateInfoSync(deviceUuid, taskUid.toString())

        val gson = GsonBuilder().setPrettyPrinting().create()
        val messageJson = gson.toJson(message)
        val result = networkUtilImpl.post(ENDPOINT_REQUEST_TASK_STATE_INFO_SYNC, messageJson)

        return result

       // return Result(false, "")
    }
}
