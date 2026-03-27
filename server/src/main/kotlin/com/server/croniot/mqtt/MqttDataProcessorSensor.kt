package com.server.croniot.mqtt

import com.server.croniot.http.SensorsDataController
import croniot.messages.MessageFactory
import croniot.messages.MessageSensorData
import croniot.models.MqttDataProcessor
import io.github.oshai.kotlinlogging.KotlinLogging

class MqttDataProcessorSensor(
    private val deviceUuid: String,
    private val sensorsDataController: SensorsDataController,
) : MqttDataProcessor {

    private val logger = KotlinLogging.logger {}

    override fun process(topic: String, data: Any) {
        try {
            val messageString = data as String
            val messageSensorData = MessageFactory.fromJson<MessageSensorData>(messageString)
            sensorsDataController.processSensorData(deviceUuid, messageSensorData)
        } catch (e: Exception) {
            logger.error(e) { "Error processing sensor data for device $deviceUuid" }
        }
    }
}
