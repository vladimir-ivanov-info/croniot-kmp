package com.server.croniot.mqtt

import ZonedDateTimeAdapter
import com.google.gson.GsonBuilder
import com.server.croniot.http.SensorsDataController
import croniot.messages.MessageSensorData
import croniot.models.MqttDataProcessor
import java.time.ZonedDateTime

class MqttDataProcessorSensor(
    private val deviceUuid: String,
    private val sensorsDataController: SensorsDataController,
) : MqttDataProcessor {

    private val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .create()

    override fun process(topic: String, data: Any) {
        try {
            val messageString = data as String
            val messageSensorData = gson.fromJson(messageString, MessageSensorData::class.java)
            sensorsDataController.processSensorData(deviceUuid, messageSensorData)
        } catch (e: Exception) {
            println("Error processing sensor data for device $deviceUuid: ${e.message}")
        }
    }
}
