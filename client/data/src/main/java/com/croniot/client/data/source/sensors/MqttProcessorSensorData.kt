package com.croniot.client.data.source.sensors

import android.util.Log
import croniot.messages.MessageFactory
import croniot.models.MqttDataProcessor
import croniot.models.dto.SensorDataDto

class MqttProcessorSensorData(
    private val onNewSensorDataDto: (SensorDataDto) -> Unit,
) : MqttDataProcessor {

    override fun process(topic: String, data: Any) {
        try {
            val message = data as String
            val sensorDataDto = MessageFactory.fromJson<SensorDataDto>(message)
            onNewSensorDataDto(sensorDataDto)
        } catch (e: Exception) {
            Log.e("MqttSensorData", "Failed to process message on topic=$topic", e)
        }
    }
}