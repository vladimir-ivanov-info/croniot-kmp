package com.croniot.android.features.device.features.sensors.data.processor

import ZonedDateTimeAdapter
import com.google.gson.GsonBuilder
import croniot.models.MqttDataProcessor
import croniot.models.dto.SensorDataDto
import org.koin.core.component.KoinComponent
import java.time.ZonedDateTime

class MqttProcessorSensorData(val onNewData : (SensorDataDto) -> Unit) : MqttDataProcessor, KoinComponent {

    val gsonZonedDateTime = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    override fun getTopic(): String {
        TODO("Not yet implemented")
    }

    override fun process(data: Any) {
        val message = data as String
        val sensorDataDto = gsonZonedDateTime.fromJson(message, SensorDataDto::class.java) //TODO handle error
        onNewData(sensorDataDto)
    }
}