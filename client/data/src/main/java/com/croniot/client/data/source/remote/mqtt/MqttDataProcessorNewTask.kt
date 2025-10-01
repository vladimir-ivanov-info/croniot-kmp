package com.croniot.client.data.source.remote.mqtt

import com.croniot.client.core.models.Task
import com.croniot.client.core.models.mappers.toModel
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.dto.TaskDto
import org.koin.core.component.KoinComponent

class MqttDataProcessorNewTask(
    val deviceUuid: String,
    private val onNewTask: (task: Task) -> Unit
) : MqttDataProcessor, KoinComponent {

    override fun process(data: Any) {
        val dataString = data as String
        val taskDto = MessageFactory.fromJsonWithZonedDateTime<TaskDto>(dataString)

        val task = taskDto.toModel()

        val finalTask = task.copy( //TODO fix from server side, add deviceUuid
            deviceUuid = deviceUuid
        )

        onNewTask(finalTask)
    }
}
