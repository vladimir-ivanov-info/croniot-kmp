package com.croniot.client.data.source.remote.mqtt

import com.croniot.client.core.models.TaskStateInfo
import com.croniot.client.core.models.mappers.toModel
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.dto.TaskStateInfoDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MqttDataProcessorTaskProgress(
    private val onNewData: (TaskStateInfo) -> Unit,
) : MqttDataProcessor, KoinComponent {

    override fun process(data: Any) {
        val dataString = data as String
        val taskStateInfoDto = MessageFactory.fromJsonWithZonedDateTime<TaskStateInfoDto>(dataString)

        val taskSateInfo = taskStateInfoDto.toModel()
        onNewData(taskSateInfo)
    }
}
