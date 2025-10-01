package com.croniot.client.core.demo

import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.SensorTypeDto

object DemoConstants {


    val SENSOR_WIFI_LEVEL_ID = 1L
    val SENSOR_WIFI_LEVEL_MIN_VALUE = -65
    val SENSOR_WIFI_LEVEL_MAX_VALUE = -50


    fun generateDemoAccountdto() : AccountDto {

        val accountDto = AccountDto(
            uuid = "account_demo",
            nickname = "Demo",
            email = "demo@email.com",
            devices = getDemoDeviceDtos()
        )

        return accountDto
    }

    private fun getDemoDeviceDtos() : List<DeviceDto> {

        val result = mutableListOf<DeviceDto>()

        val device1 = getDevice1Dto()

        result.add(device1)

        return result
    }

    fun getDevice1Dto() : DeviceDto {
        return DeviceDto(
            uuid = "IoT Device 1",
            name = "House IoT device",
            description = "IoT device for controlling sensors and running tasks in my house.",
            sensorTypes = generateSensorTypeDtos(),
            taskTypes = emptyList()
        )
    }

    private fun generateSensorTypeDtos() : List<SensorTypeDto>{

        val result : MutableList<SensorTypeDto> = mutableListOf()

        var parameters1 : MutableList<ParameterSensorDto> = mutableListOf()
        var parameterSensor1 = ParameterSensorDto(
            uid = 1,
            name =  "WiFi level",
            type = "integer",
            unit = "dBm",
            description = "WiFi level expressed in dBm",
            constraints = mapOf("minValue" to "-90", "maxValue" to "-30")
        )

        parameters1.add(parameterSensor1)

        val sensor1 = SensorTypeDto(
            uid = 1L,
            name = "WiFi level",
            description = "My IoT device's WiFi singnal level",
            parameters = parameters1
        )



        result.add(sensor1)

        return result
    }


}