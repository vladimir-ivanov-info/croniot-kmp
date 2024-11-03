package com.server.croniot

import TaskController
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.TaskProgressUpdate

class MqttDataProcessorTaskProgress(val deviceUuid: String) : MqttDataProcessor {

    override fun process(data: Any) {
        val data = data as String
        val taskProgressUpdate = MessageFactory.fromJson<TaskProgressUpdate>(data)
        println(data)
        TaskController.addTaskProgress(deviceUuid, taskProgressUpdate)
    }

    override fun getTopic(): String {
        return "esp32id_outcoming/sensor_data/2" //Should name this class "DataProcessorClientOutcoming"
    }
}