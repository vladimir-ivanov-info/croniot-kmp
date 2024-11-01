import com.google.gson.GsonBuilder
import croniot.models.Task
import com.croniot.server.db.controllers.ControllerDb
import com.google.gson.Gson
import com.server.croniot.MqttDataProcessorTaskProgress
import croniot.messages.MessageTask
import croniot.models.TaskStateInfo
import croniot.models.dto.TaskStateInfoDto
import croniot.models.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.time.ZonedDateTime


object MqttController {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val deviceMqttClient : MqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))

    val gsonZonedDateTime = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    init{
        deviceMqttClient.connect()

        initTaskStateController()
    }

    //TODO parametrize watering_system_1
    fun initTaskStateController(){
        val topic =  "/iot_to_server/task_progress_update/watering_system_1"
        val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
        var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorTaskProgress("watering_system_1", 123), topic)
    }

    //TODO unify these 2 methods in the future
    fun sendTaskToDevice(deviceUuid: String, task: Task){
        val topic = "/server/$deviceUuid/task_type/${task.taskType.uid}"

        val parametersValuesMap = mutableMapOf<Long, String>()

        for(parameter in task.parametersValues){
            parametersValuesMap.put(parameter.key.uid, parameter.value)
        }

        val messageTask = MessageTask(task.taskType.uid, parametersValuesMap, task.uid)

        val json = gson.toJson(messageTask)
        val message = MqttMessage(json.toByteArray())
        message.qos = 2 //TODO when Arduino implements compatible with QOS=2 MQTT library
        deviceMqttClient.publish(topic, message) //TODO
    }

    fun sendNewTask(deviceUuid: String, task: Task, taskStateInfo: TaskStateInfo){
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
        deviceMqttClient.publish(topic, message) //TODO
    }

    fun sendNewTaskStateInfo(deviceUuid: String, taskStateInfoDto: TaskStateInfoDto){
        CoroutineScope(Dispatchers.IO).launch {
            val topic =  "/server/task_progress_update/$deviceUuid"
            val json = gsonZonedDateTime.toJson(taskStateInfoDto)
            val message = MqttMessage(json.toByteArray())
            message.qos = 2 //TODO when Arduino implements compatible with QOS=2 MQTT library
            withContext(Dispatchers.IO) {
                deviceMqttClient.publish(topic, message) //TODO
            }
        }
    }
}