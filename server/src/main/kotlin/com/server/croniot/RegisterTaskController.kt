import croniot.models.Result
import com.croniot.server.db.controllers.ControllerDb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import croniot.messages.MessageRegisterTaskType
import croniot.models.ParameterTask

object RegisterTaskController {

    fun registerTask(messageRegisterTask: MessageRegisterTaskType) : Result {

        var result: Result

        val deviceUuid = messageRegisterTask.deviceUuid
        val deviceToken = messageRegisterTask.deviceToken

        val device = ControllerDb.deviceTokenDao.getDeviceAssociatedWithToken(deviceToken)

        if(device != null && device.uuid == deviceUuid){
            val taskType = messageRegisterTask.taskType
            taskType.device = device

            for(parameter in taskType.parameters){
                parameter.taskType = taskType
            }

            ControllerDb.taskTypeDao.insert(taskType)
            result = Result(true, "Task ${taskType.uid} registered")
        } else {
            result = Result(false, "Incorrect device or token for task register process.")
        }

        return result
    }

}