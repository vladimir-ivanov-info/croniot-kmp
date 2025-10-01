package com.croniot.client.features.tasktypes.usecases

import com.croniot.client.core.Constants.ENDPOINT_ADD_TASK
import com.croniot.client.data.source.remote.http.NetworkUtilImpl
import com.google.gson.GsonBuilder
import croniot.messages.MessageAddTask
import croniot.models.Result

class SendNewTaskUseCase(
    private val networkUtilImpl: NetworkUtilImpl
) {

    suspend operator fun invoke(deviceUuid: String, taskUid: Long, parametersValues: Map<Long, String>) : Result {
        val messageAddTask = MessageAddTask(deviceUuid, taskUid.toString(), parametersValues)

        val gson = GsonBuilder().setPrettyPrinting().create()
        val message = gson.toJson(messageAddTask)
        val result = networkUtilImpl.post(ENDPOINT_ADD_TASK, message) // 50 ms //TODO do something if result is false

        return result
    }
}
