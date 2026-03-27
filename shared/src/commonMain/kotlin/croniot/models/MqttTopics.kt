package croniot.models

object MqttTopics {
    fun newTasks(deviceUuid: String) = "/$deviceUuid/newTasks"
    fun sensorData(deviceUuid: String) = "/server_to_app/$deviceUuid/sensor_data"
    fun taskProgressWildcard(deviceUuid: String) = "/server_to_devices/$deviceUuid/task_types/+/tasks/+/progress"
    fun taskProgress(deviceUuid: String, taskTypeUid: Long, taskUid: Long) =
        "/server_to_devices/$deviceUuid/task_types/$taskTypeUid/tasks/$taskUid/progress"
}