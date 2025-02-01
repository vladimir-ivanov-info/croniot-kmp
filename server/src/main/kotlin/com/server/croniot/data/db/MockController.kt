package com.server.croniot.data.db

import com.server.croniot.data.db.controllers.ControllerDb
import croniot.models.*

// idea: add a precondition functionality before being able to run a task. For example: I cannot run the task Water plants if voltage is below certain level.

object MockController {

    val connection = ControllerDb.getConnection()

    fun addMockData() {
        val account1 = Account("account1Uuid", "vladimiriot", "email1@gmail.com", "password1", mutableSetOf())
        val device1 = Device("esp32id", "Watering system", "description123", true, /*"esp32Password",*/ mutableSetOf(), mutableSetOf(), account1)
        val device2 = Device("android1Uuid", "Android device", "description123", false, /*"androidPassword",*/ mutableSetOf(), mutableSetOf(), account1)

        val sensorType1 = SensorType(1, "WiFi signal", "WiFi signal strength expressed in dBm", mutableSetOf(), device1)
        val parameterSensor1 = ParameterSensor(1, "WiFi signal", "number", "dBm", "WiFi signal strength expressed in dBm", mutableMapOf(), sensorType1)
        val parameterSensor1Constraints = mutableMapOf("minValue" to "-100", "maxValue" to "-1")
        parameterSensor1.constraints = parameterSensor1Constraints
        sensorType1.parameters = mutableSetOf(parameterSensor1)

        val sensorType2 = SensorType(2, "Battery level", "Battery level from 0 to 100%", mutableSetOf(), device1)
        val parameterSensor2 = ParameterSensor(2, "Battery level", "number", "%", "Battery level from 0 to 100%", mutableMapOf(), sensorType2)
        val parameterSensor2Constraints = mutableMapOf("minValue" to "0", "maxValue" to "100")
        parameterSensor2.constraints = parameterSensor2Constraints
        sensorType2.parameters = mutableSetOf(parameterSensor2)

        val sensorType3 = SensorType(3, "Battery power consumption", "Battery power consumption in Wh", mutableSetOf(), device1)
        val parameterSensor3 = ParameterSensor(3, "Battery power", "number", "W", "Battery power in W", mutableMapOf(), sensorType3)
        val parameterSensor3Constraints = mutableMapOf("minValue" to "0", "maxValue" to "720")
        parameterSensor3.constraints = parameterSensor3Constraints
        sensorType3.parameters = mutableSetOf(parameterSensor3)

        val sensorType4 = SensorType(4, "Solar power", "Solar power consumption in W", mutableSetOf(), device1)
        val parameterSensor4 = ParameterSensor(4, "Solar power", "number", "W", "Solar power in W", mutableMapOf(), sensorType4)
        val parameterSensor4Constraints = mutableMapOf("minValue" to "0", "maxValue" to "100")
        parameterSensor4.constraints = parameterSensor4Constraints
        sensorType4.parameters = mutableSetOf(parameterSensor4)

        val task1 = TaskType(1, "Water plants", "Task that waters the plants", mutableSetOf(), false, device1)
        val parameterTask1 = ParameterTask(1, "minutes", "number", "m", "Number of minutes during which the plants will be watered", mutableMapOf(), task1)
        val parameterTask1Constraints = mutableMapOf("minValue" to "1", "maxValue" to "20")
        parameterTask1.constraints = parameterTask1Constraints
        task1.parameters = mutableSetOf(parameterTask1)

        device1.taskTypes.add(task1)
        device1.sensorTypes.add(sensorType1)
        device1.sensorTypes.add(sensorType2)
        device1.sensorTypes.add(sensorType3)
        device1.sensorTypes.add(sensorType4)

        account1.devices.add(device1)
        account1.devices.add(device2) // hashing problem

        // TODO  ControllerDb.accountDao.insert(account1)
    }
}
