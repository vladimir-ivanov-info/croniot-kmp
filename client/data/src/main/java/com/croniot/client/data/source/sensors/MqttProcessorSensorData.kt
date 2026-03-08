package com.croniot.client.data.source.sensors

import ZonedDateTimeAdapter
import com.google.gson.GsonBuilder
import croniot.models.MqttDataProcessor
import croniot.models.dto.SensorDataDto
import java.time.ZonedDateTime

class MqttProcessorSensorData(
    private val onNewSensorDataDto: (SensorDataDto) -> Unit,
) : MqttDataProcessor {

    val gsonZonedDateTime = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    override fun process(topic: String, data: Any) {
        val message = data as String
        val sensorDataDto = gsonZonedDateTime.fromJson(message, SensorDataDto::class.java) // TODO handle error
        onNewSensorDataDto(sensorDataDto)
    }
}
