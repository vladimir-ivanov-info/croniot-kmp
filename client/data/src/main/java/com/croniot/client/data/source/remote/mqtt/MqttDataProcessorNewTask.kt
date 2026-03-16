package com.croniot.client.data.source.remote.mqtt

import android.util.Log
import com.croniot.client.core.mappers.toModel
import com.croniot.client.core.models.Task
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.dto.TaskDto

class MqttDataProcessorNewTask(
    val deviceUuid: String,
    private val onNewTask: (task: Task) -> Unit,
) : MqttDataProcessor {

    override fun process(topic: String, data: Any) {
        try {
            val dataString = data as String
            val taskDto = MessageFactory.fromJsonWithZonedDateTime<TaskDto>(dataString)

            val task = taskDto.toModel()

            val finalTask = task.copy(
                deviceUuid = deviceUuid,
            )

            Log.d("RTT", "MQTT newTask received: state=${finalTask.initialTaskStateInfo?.state} (topic: $topic)")
            onNewTask(finalTask)
        } catch (e: Exception) {
            Log.e("MqttNewTask", "Failed to process message on topic=$topic", e)
        }
    }
}