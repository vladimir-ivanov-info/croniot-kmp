import croniot.models.*
import croniot.models.dto.TaskDto
import com.croniot.server.db.controllers.ControllerDb
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

object TaskController {

    init {
        val devices = ControllerDb.deviceDao.getAll();

        for(device in devices){
            val deviceUuid = device.uuid
            val topic =  "/iot_to_server/task_progress_update/$deviceUuid"
            val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
            var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid, 123), topic)
        }
    }

    fun addTaskProgress(deviceUuid: String, taskProgressUpdate: TaskProgressUpdate){
        val taskUid = taskProgressUpdate.taskUid
        val taskTypeUid = taskProgressUpdate.taskTypeUid
        val taskProgress = taskProgressUpdate.progress
        val taskState = taskProgressUpdate.state
        val errorMessage = taskProgressUpdate.errorMessage

        val device = ControllerDb.deviceDao.getByUuid(deviceUuid)

        if(device != null && taskUid != null){
            val taskTypeExists = ControllerDb.taskTypeDao.exists(device, taskTypeUid) // 8 ms

            if(taskTypeExists){
                val task = ControllerDb.taskDao.getLazy(deviceUuid, taskTypeUid, taskUid) //1700 ms -> 1500 ms -> 3 ms
                if(task != null){
                    val taskStateEnum = stateStringToEnum(taskState)
                    val stateInfo = TaskStateInfo(ZonedDateTime.now(), taskStateEnum, taskProgress,  errorMessage, task)

                    ControllerDb.taskStateInfoDao.insert(stateInfo) //5-7 ms

                    val stateInfoDto = stateInfo.toDto()  //1816 ms
                    MqttController.sendNewTaskStateInfo(deviceUuid, stateInfoDto) //100-120 ms -> 90-100 -> 1 ms
                }
            }
        }
    }

    fun getTasksByDeviceUuid(deviceUuid: String) : List<TaskDto> {
        val tasks = ControllerDb.taskDao.getAll(deviceUuid)
        val tasksDto = tasks.map { it.toDto() }.toMutableList()
        return tasksDto
    }

    private fun stateStringToEnum(state: String) : TaskState {

       var taskState : TaskState = TaskState.UNDEFINED

        if(state == "CREATED"){
            taskState = TaskState.CREATED
        } else if(state == "RECEIVED"){
            taskState = TaskState.RECEIVED
        } else if(state == "RUNNING") {
            taskState = TaskState.RUNNING
        }else if(state == "COMPLETED") {
            taskState = TaskState.COMPLETED
        }

        return taskState
    }
}