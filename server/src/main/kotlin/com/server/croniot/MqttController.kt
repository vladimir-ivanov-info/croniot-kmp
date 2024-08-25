import com.google.gson.GsonBuilder
import croniot.models.Task
import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageTask
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage


object MqttController {

    val devicesMqttClients = mutableMapOf<String, MqttClient>()

    fun init(){
        val devices = ControllerDb.deviceDao.getAll();

        for(device in devices){

            val deviceUuid = device.uuid

            for(sensor in device.sensorTypes){
                val sensorUid = sensor.uid
                val topic = deviceUuid + "_outcoming/sensor_data/" + sensorUid
                val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
                var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid, sensorUid), topic)
            }


            //TODO for task in device.tasks create "_incoming" topic and store a map of <deviceUuid, mqttclient>
//            for(task in device.taskTypes){
//               // val taskUid = task.uid
//               // val topic = deviceUuid + "_incoming/task_configuration/" + taskUid
//                val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
//                val topic = deviceUuid + "_incoming/task/" + task
//                devicesMqttClients.put(device.uuid, mqttClient)
//                var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorTask(deviceUuid, sensorUid), topic)
//            }
        }


//        for(sensorInfo in sensorInfoList){
//            val uuid = sensorInfo.uuid
//            val sensorId = sensorInfo.id
//        //TODO topic mal, falta uuid en vez de id al principio
//            val topic = uuid + "_outcoming/sensor_data/" + sensorId
//
//            val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
//            var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorSensor(uuid, sensorId), topic)
//        }

    }

    fun sendTaskToDevice(deviceUuid: String, task: Task){
        val deviceMqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
        deviceMqttClient.connect()
        // if(deviceMqttClient != null){
            val taskUid = task.taskType.uid
            val topic = "/server/$deviceUuid/task_type/${task.taskType.uid}"


            val parametersValuesMap = mutableMapOf<Long, String>()

            for(parameter in task.parametersValues){
                parametersValuesMap.put(parameter.key.uid, parameter.value)
            }

            val messageTask = MessageTask(task.taskType.uid, parametersValuesMap, task.uid)

            val json = GsonBuilder().setPrettyPrinting().create().toJson(messageTask)
            val message = MqttMessage(json.toByteArray())
            deviceMqttClient.publish(topic, message) //TODO
       // }
    }

}