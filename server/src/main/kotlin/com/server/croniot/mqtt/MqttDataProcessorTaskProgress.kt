package com.server.croniot.mqtt

import com.server.croniot.controllers.TaskController
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.TaskProgressUpdate

class MqttDataProcessorTaskProgress(
    private val deviceUuid: String,
    private val taskController: TaskController,
) : MqttDataProcessor {

    override fun process(topic: String, data: Any) {
        try {
            val message = data as String
            val taskProgressUpdate = MessageFactory.fromJson<TaskProgressUpdate>(message)
            taskController.addTaskProgress(deviceUuid, taskProgressUpdate)
        } catch (e: Exception) {
            println("Error processing task progress for device $deviceUuid: ${e.message}")
        }
    }
}
