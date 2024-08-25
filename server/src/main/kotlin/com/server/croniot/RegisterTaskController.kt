import croniot.models.Result
import com.croniot.server.db.controllers.ControllerDb
import croniot.messages.MessageRegisterTaskType

object RegisterTaskController {

    fun registerTask(messageRegisterTask: MessageRegisterTaskType) : Result {

        var result = Result(false, "")

        val deviceUuid = messageRegisterTask.deviceUuid
        val deviceToken = messageRegisterTask.deviceToken

        val device = ControllerDb.deviceTokenDao.getDeviceAssociatedWithToken(deviceToken)

        if(device != null && device.uuid == deviceUuid){
            val task = messageRegisterTask.taskType
            task.device = device


            for(parameter in task.parameters){
                parameter.taskType = task
            }

            ControllerDb.taskTypeDao.insert(task)
            result = Result(true, "Task ${task.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token for task register process.")
        }

        return result
    }

}