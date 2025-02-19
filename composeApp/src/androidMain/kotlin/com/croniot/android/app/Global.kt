package com.croniot.android.app

import croniot.models.dto.DeviceDto
import croniot.models.dto.TaskTypeDto
import java.util.*

object Global {

    // TODO move to core/util/Constants

    val appName = "croniot"

    val SERVER_ADDRESS_LOCAL = "192.168.50.163"
    var SERVER_ADDRESS_REMOTE = "51.77.195.204"

    var SERVER_ADDRESS = "192.168.50.163"
    var SERVER_PORT = 8090

    var mqttBrokerUrl = "tcp://51.77.195.204:1883"
    val mqttClientId = "AndroidMQTTClient"

    // TODO move to a separate util class
    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        val uniqueString = uuid.toString().substring(0, length)
        return uniqueString
    }

    var selectedDevice: DeviceDto? = null
    var selectedTaskType: TaskTypeDto? = null
}
