package com.server.croniot.mqtt

import Global
import MqttHandler
import com.server.croniot.data.mappers.toDto
import croniot.measure
import com.server.croniot.di.DI
import croniot.messages.MessageFactory
import croniot.messages.MessageTask
import croniot.models.Device
import croniot.models.MqttTopics
import croniot.models.Task
import croniot.models.dto.SensorDataDto
import croniot.models.dto.TaskStateInfoDto
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
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import io.github.oshai.kotlinlogging.KotlinLogging

object MqttController {

    private val logger = KotlinLogging.logger {}

    private val deviceMqttClient: MqttClient = MqttClient(
        Global.secrets.mqttBrokerUrl,
        Global.secrets.mqttClientId + Global.generateUniqueString(8),
        MemoryPersistence(),
    )

    private val clientLock = Mutex()
    private val deviceClients = mutableListOf<MqttClient>()

    init {
        deviceMqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                logger.warn { "MQTT connection lost: ${cause?.message}" }
                CoroutineScope(Dispatchers.IO).launch {
                    while (!deviceMqttClient.isConnected) {
                        try {
                            logger.info { "Attempting MQTT reconnection..." }
                            deviceMqttClient.connect()
                            logger.info { "MQTT reconnected to broker" }
                        } catch (e: MqttException) {
                            logger.warn { "MQTT reconnection failed: ${e.message}. Retrying in 5s..." }
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
                    MemoryPersistence(),
                )
                synchronized(deviceClients) { deviceClients.add(mqttClient) }

                val taskController = DI.appComponent.taskController()
                MqttHandler(
                    mqttClient,
                    MqttDataProcessorTaskProgress(device.uuid, taskController),
                    topic,
                    CoroutineScope(Dispatchers.IO)
                )
            }
        }
    }

    fun listenToNewDevice(device: Device) {
        val topic = "/iot_to_server/task_progress_update/${device.uuid}"
        val mqttClient = MqttClient(
            Global.secrets.mqttBrokerUrl,
            Global.secrets.mqttClientId + Global.generateUniqueString(8),
            MemoryPersistence(),
        )
        synchronized(deviceClients) { deviceClients.add(mqttClient) }

        val taskController = DI.appComponent.taskController()
        MqttHandler(
            mqttClient,
            MqttDataProcessorTaskProgress(device.uuid, taskController),
            topic,
            CoroutineScope(Dispatchers.IO)
        )
    }

    suspend fun sendTaskToDevice(deviceUuid: String, task: Task) {
        val t0 = System.nanoTime()
        clientLock.withLock {
            val waitMs = (System.nanoTime() - t0) / 1_000_000
            val topic = "/server/$deviceUuid/task_type/${task.taskTypeUid}"

            val parametersValuesMap = mutableMapOf<Long, String>()
            for (parameter in task.parametersValues) {
                parametersValuesMap.put(parameter.key.uid, parameter.value)
            }

            val messageTask = MessageTask(task.taskTypeUid, parametersValuesMap, task.uid)

            val json = MessageFactory.toJson(messageTask)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            //measure("### sendTaskToDevice publish") {
                deviceMqttClient.publish(topic, message)
            //}
            logger.debug { "sendTaskToDevice lockWait=${waitMs}ms" }
        }
    }

    suspend fun sendNewTask(deviceUuid: String, task: Task) {
        val t0 = System.nanoTime()
        clientLock.withLock {
            val waitMs = (System.nanoTime() - t0) / 1_000_000
            val topic = MqttTopics.newTasks(deviceUuid)
            val taskDto = task.toDto()
            val json = MessageFactory.toJson(taskDto)

            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            //measure("### sendNewTask publish") {
                deviceMqttClient.publish(topic, message)
            //}
            logger.debug { "sendNewTask lockWait=${waitMs}ms" }
        }
    }

    suspend fun sendSensorData(sensorDataDto: SensorDataDto) {
        val t0 = System.nanoTime()
        clientLock.withLock {
            val waitMs = (System.nanoTime() - t0) / 1_000_000
            val topic = MqttTopics.sensorData(sensorDataDto.deviceUuid)
            val json = MessageFactory.toJson(sensorDataDto)

            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            //measure("### sendSensorData publish") {
                deviceMqttClient.publish(topic, message)
            //}
            //println("### sendSensorData lockWait=${waitMs}ms")
        }
    }

    suspend fun sendNewTaskStateInfo(
        deviceUuid: String,
        taskTypeUid: Long,
        taskUid: Long,
        payload: TaskStateInfoDto
    ) {
        val t0 = System.nanoTime()
        clientLock.withLock {
            val waitMs = (System.nanoTime() - t0) / 1_000_000
            val topic = MqttTopics.taskProgress(deviceUuid, taskTypeUid, taskUid)

            val json = MessageFactory.toJson(payload)
            val message = MqttMessage(json.toByteArray()).apply {
                qos = 2
                isRetained = false
            }

            //measure("### sendNewTaskStateInfo publish") {
                deviceMqttClient.publish(topic, message)
            //}
            //println("### sendNewTaskStateInfo lockWait=${waitMs}ms")
            //println("[RTT] sendNewTaskStateInfo published to: $topic (${json.length} bytes)")
        }
    }

    suspend fun requestTaskStateInfoSync(deviceUuid: String, taskTypeId: Long) {
        clientLock.withLock {
            val topic = "/server/$deviceUuid/task_state_info_sync/$taskTypeId"
            val json = MessageFactory.toJson(taskTypeId)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained = false
            deviceMqttClient.publish(topic, message)
        }
    }
}
