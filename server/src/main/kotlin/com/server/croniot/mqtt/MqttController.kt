package com.server.croniot.mqtt

import Global
import MqttHandler
import ZonedDateTimeAdapter
import com.google.gson.GsonBuilder
import croniot.models.Task
import com.google.gson.Gson
import com.server.croniot.application.AppComponent
import com.server.croniot.application.DaggerAppComponent
import croniot.messages.MessageTask
import croniot.models.Device
import croniot.models.TaskStateInfo
import croniot.models.dto.SensorDataDto
import croniot.models.dto.TaskStateInfoDto
import croniot.models.toDto
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
    private val deviceMqttClient : MqttClient = MqttClient(
        Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(
            8
        )
    )

    private val clientLock = Mutex()

    val gsonZonedDateTime = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    init{
        deviceMqttClient.setCallback(object : MqttCallback{
            override fun connectionLost(cause: Throwable?) {
                println("Connection lost: ${cause?.message}. Attempting to reconnect...")
                CoroutineScope(Dispatchers.IO).launch {
                    while (!deviceMqttClient.isConnected) {
                        try {
                            println("Attempting to reconnect to the broker...")
                            deviceMqttClient.connect()
                            println("Reconnected to broker.")
                            //client.subscribe("your/topic") // Subscribe again after reconnecting
                        } catch (e: MqttException) {
                            println("Reconnection failed: ${e.message}. Retrying in 5 seconds...")
                            delay(5000) // Wait before retrying
                        }
                    }
                }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
               // TODO("Not yet implemented")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                //TODO("Not yet implemented")
            }

        })

        deviceMqttClient.connect()

        initTaskStateController()
    }

    //TODO store these MQTT connections in a class variable so we can stop them later
    fun initTaskStateController(){
        val appComponent: AppComponent = DaggerAppComponent.create()

        val devices = appComponent.deviceRepository().getAll()
        val iotDevices = devices.filter { it.iot }

        for(device in iotDevices){
            val topic =  "/iot_to_server/task_progress_update/${device.uuid}"
            val mqttClient = MqttClient(
                Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8)
            )

            val taskController = appComponent.taskController()
            MqttHandler(mqttClient, MqttDataProcessorTaskProgress(device.uuid, taskController), topic)
        }
    }

    //TODO store these MQTT connections in a class variable so we can stop them later
    fun listenToNewDevice(device: Device){
        val topic =  "/iot_to_server/task_progress_update/${device.uuid}"
        val mqttClient = MqttClient(
            Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(
                8
            )
        )

        //TODOO
        val appComponent: AppComponent = DaggerAppComponent.create()
        val taskController = appComponent.taskController()
        MqttHandler(mqttClient, MqttDataProcessorTaskProgress(device.uuid, taskController), topic)
    }

    //TODO unify these 2 methods in the future
    suspend fun sendTaskToDevice(deviceUuid: String, task: Task){
        clientLock.withLock {
            val topic = "/server/$deviceUuid/task_type/${task.taskType.uid}"

            val parametersValuesMap = mutableMapOf<Long, String>()

            for(parameter in task.parametersValues){
                parametersValuesMap.put(parameter.key.uid, parameter.value)
            }

            val messageTask = MessageTask(task.taskType.uid, parametersValuesMap, task.uid)

            val json = gson.toJson(messageTask)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2 //TODO when Arduino implements compatible with QOS=2 MQTT library
            message.isRetained  = false
            deviceMqttClient.publish(topic, message) //TODO
        }
    }

    suspend fun sendNewTask(deviceUuid: String, task: Task, taskStateInfo: TaskStateInfo){
        clientLock.withLock {
            val topic = "/$deviceUuid/newTasks"

            val taskDto = task.toDto()
            taskDto.stateInfos.add(taskStateInfo.toDto())

            val gson2 = GsonBuilder()
                .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
                .setPrettyPrinting()
                .create()

            val json = gson2.toJson(taskDto)

            val message = MqttMessage(json.toByteArray())
            message.qos = 2 //TODO when Arduino implements compatible with QOS=2 MQTT library
            message.isRetained  = false
            deviceMqttClient.publish(topic, message) //TODO
        }

    }

    suspend fun sendSensorData(sensorDataDto: SensorDataDto){
        clientLock.withLock {
            val topic = "/server_to_app/${sensorDataDto.deviceUuid}/sensor_data"

            val json = gsonZonedDateTime.toJson(sensorDataDto)

            val message = MqttMessage(json.toByteArray())
            message.qos = 2 //TODO when Arduino implements compatible with QOS=2 MQTT library
            message.isRetained = false
            deviceMqttClient.publish(topic, message) //TODO
        }
    }

    suspend fun sendNewTaskStateInfo(deviceUuid: String, taskStateInfoDto: TaskStateInfoDto){
        clientLock.withLock {
            val topic = "/server_to_devices/task_progress_update/$deviceUuid"
            val json = gsonZonedDateTime.toJson(taskStateInfoDto)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2
            message.isRetained  = false
            deviceMqttClient.publish(topic, message) //TODO
        }
    }
}