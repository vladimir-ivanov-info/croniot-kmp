package com.server.croniot.mqtt

import com.server.croniot.controllers.TaskController
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.TaskProgressUpdate
import javax.inject.Inject

class MqttDataProcessorTaskProgress @Inject constructor(
    val deviceUuid: String,
    private val taskController: TaskController
): MqttDataProcessor {

    override fun process(data: Any) {
        val data = data as String

        val taskProgressUpdate = MessageFactory.fromJson<TaskProgressUpdate>(data)
        println(data)

        taskController.addTaskProgress(deviceUuid, taskProgressUpdate)
    }
}