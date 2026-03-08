package com.croniot.client.data.source.remote.mqtt

import com.croniot.client.core.models.events.TaskStateInfoEvent
import com.croniot.client.core.mappers.toModel
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.TaskKey
import croniot.models.dto.TaskStateInfoDto
import org.koin.core.component.KoinComponent

class MqttDataProcessorTaskProgress(
    private val onNewData: (TaskStateInfoEvent) -> Unit,
) : MqttDataProcessor, KoinComponent {

    override fun process(topic: String, data: Any) {
        val dataString = data as String

        val key = parseProgressTopic(topic) ?: return

        val dto = MessageFactory.fromJsonWithZonedDateTime<TaskStateInfoDto>(dataString)
        val info = dto.toModel()

        onNewData(TaskStateInfoEvent(key = key, info = info))
    }

    private fun parseProgressTopic(topic: String): TaskKey? {
        val parts = topic.trim('/').split('/')
        // server_to_devices/{deviceUuid}/task_types/{taskTypeUid}/tasks/{taskUid}/progress
        if (parts.size != 7) return null
        if (parts[0] != "server_to_devices") return null
        if (parts[2] != "task_types") return null
        if (parts[4] != "tasks") return null
        if (parts[6] != "progress") return null

        val deviceUuid = parts[1]
        val taskTypeUid = parts[3].toLongOrNull() ?: return null
        val taskUid = parts[5].toLongOrNull() ?: return null

        return TaskKey(deviceUuid, taskTypeUid, taskUid)
    }
}
