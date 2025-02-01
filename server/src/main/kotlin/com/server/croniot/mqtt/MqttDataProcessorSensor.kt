package com.server.croniot.mqtt

import ZonedDateTimeSerializer
import com.google.gson.GsonBuilder
import com.server.croniot.http.SensorsDataController
import croniot.messages.MessageSensorData
import croniot.models.MqttDataProcessor
import java.time.ZonedDateTime

class MqttDataProcessorSensor(val deviceUuid: String, val sensorsDataController: SensorsDataController) : MqttDataProcessor {

    val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeSerializer())
        .create()

    override fun process(data: Any) {
        val messageString = data as String
        // TODO handle error in fromJson
        val messageSensorData = gson.fromJson(messageString, MessageSensorData::class.java)

        sensorsDataController.processSensorData(deviceUuid, messageSensorData)
    }
}
