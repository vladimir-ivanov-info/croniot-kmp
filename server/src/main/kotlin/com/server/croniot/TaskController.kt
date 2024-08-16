import croniot.models.*
import croniot.models.dto.TaskDto
import com.croniot.server.db.controllers.ControllerDb
import java.time.ZonedDateTime

object TaskController {

    fun getTasksByDeviceUuid(deviceUuid: String) : List<TaskDto> {
        val tasks = ControllerDb.taskDao.getAll(deviceUuid)
        val tasksDto = tasks.map { it.toDto() }.toMutableList()
        return tasksDto
    }

    fun stateStringToEnum(state: String) : TaskState{

       var taskState : TaskState = TaskState.UNDEFINED

        if(state == "CREATED"){
            taskState = TaskState.CREATED
        } else if(state == "RECEIVED"){
            taskState = TaskState.RECEIVED
        } else if(state == "COMPLETED") {
            taskState = TaskState.COMPLETED
        }

        return taskState
    }

    fun addTaskState(deviceUuid: String, taskTypeUid: Long, taskUid: Long, state: String){

        val device = ControllerDb.deviceDao.getByUuid(deviceUuid)

        if(device != null && taskUid != null){

            val taskTypeObj = ControllerDb.taskTypeDao.get(device, taskTypeUid)

            if(taskTypeObj != null){

                val task = ControllerDb.taskDao.get(deviceUuid, taskTypeUid, taskUid)

                if(task != null){
                    val taskState = stateStringToEnum(state)
                    val stateInfo = TaskStateInfo(ZonedDateTime.now(), taskState, "", task)
                    ControllerDb.taskStateInfoDao.insert(stateInfo)
                }
            }
        }
    }
}