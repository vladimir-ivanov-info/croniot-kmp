import croniot.models.*
import croniot.models.dto.TaskDto
import com.croniot.server.db.controllers.ControllerDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttClient
import java.time.ZonedDateTime

object TaskController {

    init {
        val devices = ControllerDb.deviceDao.getAll();

        for(device in devices){
            val deviceUuid = device.uuid
            val topic =  "/iot_to_server/task_progress_update/$deviceUuid"
            //val mqttClient = MqttClient(Global.secrets.mqttBrokerUrl, Global.secrets.mqttClientId + Global.generateUniqueString(8))
            //var mqttHandler = MqttHandler(mqttClient, MqttDataProcessorSensor(deviceUuid, 123), topic)
        }
    }

    fun addTaskProgress(deviceUuid: String, taskProgressUpdate: TaskProgressUpdate){
        try {
            val startMillis = System.currentTimeMillis()

            val taskUid = taskProgressUpdate.taskUid
            val taskTypeUid = taskProgressUpdate.taskTypeUid
            val taskProgress = taskProgressUpdate.progress
            val taskState = taskProgressUpdate.state
            val errorMessage = taskProgressUpdate.errorMessage

            val device = ControllerDb.deviceDao.getByUuid(deviceUuid)

            if (device != null && taskUid != null) {
                val taskTypeExists = ControllerDb.taskTypeDao.exists(device, taskTypeUid) // 8 ms

                if (taskTypeExists) {
                    if (taskUid.toInt() == -1) {
                        val taskType = ControllerDb.taskTypeDao.get(device, taskTypeUid)
                        taskType?.let {
                            val task = ControllerDb.taskDao.create(device, taskType)

                            task?.let {
                                val taskStateEnum = taskState
                                val stateInfo = TaskStateInfo(
                                    ZonedDateTime.now(),
                                    taskStateEnum,
                                    taskProgress,
                                    errorMessage,
                                    task
                                )
                                ControllerDb.taskStateInfoDao.insert(stateInfo) //5-7 ms

                                val stateInfoDto = stateInfo.toDto()  //1816 ms
                                println("Time task state: ${stateInfoDto.state} ${ZonedDateTime.now()}")
                                CoroutineScope(Dispatchers.IO).launch {
                                    MqttController.sendNewTask(deviceUuid, task, stateInfo)
                                }
                            }
                        }
                    } else {
                        val task = ControllerDb.taskDao.getLazy(deviceUuid, taskTypeUid, taskUid) //1700 ms -> 1500 ms -> 3 ms
                        task?.let {
                            val stateInfo = TaskStateInfo(
                                ZonedDateTime.now(),
                                taskState,
                                taskProgress,
                                errorMessage,
                                task
                            )

                            ControllerDb.taskStateInfoDao.insert(stateInfo) //5-7 ms

                            val stateInfoDto = stateInfo.toDto()  //1816 ms
                            CoroutineScope(Dispatchers.IO).launch {
                                MqttController.sendNewTaskStateInfo(deviceUuid, stateInfoDto) //100-120 ms -> 90-100 -> 1 ms
                            }
                            //println("Millis 4: ${System.currentTimeMillis()} ${stateInfoDto.state}")
                        }
                    }
                }
            }
        } catch (e : Exception){
            e.printStackTrace()
        }
    }

    fun getTasksByDeviceUuid(deviceUuid: String) : List<TaskDto> {
        val tasks = ControllerDb.taskDao.getAll(deviceUuid)
        val tasksDto = tasks.map { it.toDto() }.toMutableList()

        println(tasksDto)

        return tasksDto
    }

}