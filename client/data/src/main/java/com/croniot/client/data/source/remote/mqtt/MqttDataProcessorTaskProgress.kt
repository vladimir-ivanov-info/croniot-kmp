package com.croniot.client.data.source.remote.mqtt

import android.util.Log
import com.croniot.client.data.mappers.toModel
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.TaskKey
import croniot.models.dto.TaskStateInfoDto

class MqttDataProcessorTaskProgress(
    private val onNewData: (TaskStateInfoEvent) -> Unit,
) : MqttDataProcessor {

    override fun process(topic: String, data: Any) {
        try {
            val dataString = data as String

            val key = parseTaskStateInfoTopic(topic) ?: return

            val dto = MessageFactory.fromJsonWithZonedDateTime<TaskStateInfoDto>(dataString)
            val info = dto.toModel()

            Log.d("RTT", "MQTT received: ${info.state} (topic: $topic)")
            onNewData(TaskStateInfoEvent(key = key, info = info))
        } catch (e: Exception) {
            Log.e("MqttTaskStateInfo", "Failed to process message on topic=$topic", e)
        }
    }

    private fun parseTaskStateInfoTopic(topic: String): TaskKey? {
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