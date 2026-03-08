package com.server.croniot.mqtt

import Global
import MqttHandler
import ZonedDateTimeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.server.croniot.di.DI
import croniot.messages.MessageTask
import croniot.models.Device
import croniot.models.Task
import croniot.models.dto.SensorDataDto
import croniot.models.dto.TaskStateInfoDto
import com.server.croniot.data.mappers.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.time.ZonedDateTime

object MqttController {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val gsonZonedDateTime: Gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    private val deviceMqttClient: MqttClient = MqttClient(
        Global.secrets.mqttBrokerUrl,
        Global.secrets.mqttClientId + Global.generateUniqueString(8),
    )

    private val clientLock = Mutex()
    private val deviceClients = mutableListOf<MqttClient>()

    init {
        deviceMqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                println("Connection lost: ${cause?.message}. Attempting to reconnect...")
                CoroutineScope(Dispatchers.IO).launch {
                    while (!deviceMqttClient.isConnected) {
                        try {
                            println("Attempting to reconnect to the broker...")
                            deviceMqttClient.connect()
                            println("Reconnected to broker.")
                        } catch (e: MqttException) {
                            println("Reconnection failed: ${e.message}. Retrying in 5 seconds...")
                            delay(5000)
                        }
                    }
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {}

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        deviceMqttClient.connect()

        initTaskStateController()
    }

    fun initTaskStateController() {
        val devices = DI.appComponent.deviceRepository().getAll()
        val iotDevices = devices.filter { it.iot }

        for (device in iotDevices) {
            CoroutineScope(Dispatchers.IO).launch {
                val topic = "/iot_to_server/task_progress_update/${device.uuid}"
                val mqttClient = MqttClient(
                    Global.secrets.mqttBrokerUrl,
                    Global.secrets.mqttClientId + Global.generateUniqueString(8),
                )
                synchronized(deviceClients) { deviceClients.add(mqttClient) }

                val taskController = DI.appComponent.taskController()
                MqttHandler(mqttClient, MqttDataProcessorTaskProgress(device.uuid, taskController), topic)
            }
        }
    }

    fun listenToNewDevice(device: Device) {
        val topic = "/iot_to_server/task_progress_update/${device.uuid}"
        val mqttClient = MqttClient(
            Global.secrets.mqttBrokerUrl,
            Global.secrets.mqttClientId + Global.generateUniqueString(8),
        )
        synchronized(deviceClients) { deviceClients.add(mqttClient) }

        val taskController = DI.appComponent.taskController()
        MqttHandler(mqttClient, MqttDataProcessorTaskProgress(device.uuid, taskController), topic)
    }

    suspend fun sendTaskToDevice(deviceUuid: String, task: Task) {
        clientLock.withLock {
            val topic = "/server/$deviceUuid/task_type/${task.taskTypeUid}"

            val parametersValuesMap = mutableMapOf<Long, String>()
            for (parameter in task.parametersValues) {
                parametersValuesMap.put(parameter.key.uid, parameter.value)
            }

            val messageTask = MessageTask(task.taskTypeUid, parametersValuesMap, task.uid)

            val json = gson.toJson(messageTask)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            deviceMqttClient.publish(topic, message)
        }
    }

    suspend fun sendNewTask(deviceUuid: String, task: Task) {
        clientLock.withLock {
            val topic = "/$deviceUuid/newTasks"
            val taskDto = task.toDto()
            val json = gsonZonedDateTime.toJson(taskDto)

            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            deviceMqttClient.publish(topic, message)
        }
    }

    suspend fun sendSensorData(sensorDataDto: SensorDataDto) {
        clientLock.withLock {
            val topic = "/server_to_app/${sensorDataDto.deviceUuid}/sensor_data"
            val json = gsonZonedDateTime.toJson(sensorDataDto)

            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            deviceMqttClient.publish(topic, message)
        }
    }

    suspend fun sendNewTaskStateInfo(
        deviceUuid: String,
        taskTypeUid: Long,
        taskUid: Long,
        payload: TaskStateInfoDto
    ) {
        clientLock.withLock {
            val topic =
                "/server_to_devices/$deviceUuid/task_types/$taskTypeUid/tasks/$taskUid/progress"

            val json = gsonZonedDateTime.toJson(payload)
            val message = MqttMessage(json.toByteArray()).apply {
                qos = 2
                isRetained = false
            }

            deviceMqttClient.publish(topic, message)
        }
    }

    suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeId: Long) {
        clientLock.withLock {
            val topic = "/server/$deviceUuid/task_state_info_sync/$taskTypeId"
            val json = gsonZonedDateTime.toJson(taskTypeId)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            deviceMqttClient.publish(topic, message)
        }
    }
}
