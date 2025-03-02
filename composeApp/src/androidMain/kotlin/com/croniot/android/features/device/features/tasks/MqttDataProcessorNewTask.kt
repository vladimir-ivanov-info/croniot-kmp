package com.croniot.android.features.device.features.tasks

import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.dto.TaskDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MqttDataProcessorNewTask(val deviceUuid: String) : MqttDataProcessor, KoinComponent {

    private val viewModelTasks: ViewModelTasks = get()

    override fun process(data: Any) {
        val dataString = data as String
        val newTask = MessageFactory.fromJsonWithZonedDateTime<TaskDto>(dataString)
        viewModelTasks.addTask(newTask)
    }
}
