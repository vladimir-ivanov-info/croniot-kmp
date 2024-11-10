import croniot.models.ParameterTask
import croniot.models.Task
import croniot.models.TaskState
import croniot.models.TaskStateInfo
import croniot.models.Result
import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageAddTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.random.Random

object AddTaskController {

    fun addTask(messageAddTask: MessageAddTask) : Result {
        val deviceUuid = messageAddTask.deviceUuid
        val taskTypeUid = messageAddTask.taskTypeUid
        val parametersValues = messageAddTask.parametersValues

        val device = ControllerDb.deviceDao.getLazy(deviceUuid) //2482 ms -> 1089 ms without logback -> 32 ms with val query = sess.createQuery(cr).uniqueResultOptional()

        if(device != null){

            val taskType = ControllerDb.taskTypeDao.getLazy(device, taskTypeUid.toLong()) //53-129 ms ->  2-11ms

            if(taskType != null){
                val parametersValuesForDatabase = mutableMapOf<ParameterTask, String>()
                for(parameterValueEntry in parametersValues){
                    val parameterUid = parameterValueEntry.key
                    val value = parameterValueEntry.value

                    val parameterTask = ControllerDb.parameterTaskDao.getByUid(parameterUid, taskType) //60-149 ms per parameter -> 7 ms

                    if(parameterTask != null){
                        parametersValuesForDatabase[parameterTask] = value
                    }
                }

                val taskUid = Random.nextLong(1, 10001)

                val task = Task(taskUid, parametersValuesForDatabase, taskType, mutableSetOf())

                ControllerDb.taskDao.insert(task) //7-47 ms

                val taskStateInfo = TaskStateInfo(ZonedDateTime.now(), TaskState.CREATED, 0.0, "", task) //TODO check if taskConfiguration.id gets updated from 0 to actual value

                ControllerDb.taskStateInfoDao.insert(taskStateInfo) //4-6 ms

                CoroutineScope(Dispatchers.IO).launch {
                    MqttController.sendNewTask(deviceUuid, task, taskStateInfo)
                    MqttController.sendTaskToDevice(deviceUuid, task) //468-620 ms  ->  99-616 ms
                }
            }
        }

        print(deviceUuid)
        return Result(false, "")
    }

}