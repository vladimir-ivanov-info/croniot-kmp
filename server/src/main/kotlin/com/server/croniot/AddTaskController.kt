import croniot.models.ParameterTask
import croniot.models.Task
import croniot.models.TaskState
import croniot.models.TaskStateInfo
import croniot.models.Result
import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageAddTask
import java.time.ZonedDateTime
import kotlin.random.Random

object AddTaskController {

    fun registerTask(messageAddTask: MessageAddTask) : Result {
        val deviceUuid = messageAddTask.deviceUuid
        val taskUid = messageAddTask.taskUid
        val parametersValues = messageAddTask.parametersValues

        val device = ControllerDb.deviceDao.getByUuid(deviceUuid)

        if(device != null){
            val task = ControllerDb.taskTypeDao.get(device, taskUid.toLong())

            if(task != null){

                val parametersValuesForDatabase = mutableMapOf<ParameterTask, String>()

                for(parameterValueEntry in parametersValues){
                    val parameterUid = parameterValueEntry.key
                    val value = parameterValueEntry.value

                    val parameterTask = ControllerDb.parameterTaskDao.getByUid(parameterUid, task)

                    if(parameterTask != null){
                        parametersValuesForDatabase.put(parameterTask, value)
                    }
                }

                val taskUid = Random.nextLong(1, 10001)

                val task = Task(taskUid, parametersValuesForDatabase, task, mutableSetOf())
                ControllerDb.taskDao.insert(task)

                val stateInfo = TaskStateInfo(ZonedDateTime.now(), TaskState.CREATED, "", task) //TODO check if taskConfiguration.id gets updated from 0 to actual value
                ControllerDb.taskStateInfoDao.insert(stateInfo)

                MqttController.sendTaskToDevice(deviceUuid, task)
            }
        }

        print(deviceUuid)
        return Result(false, "")
    }

}