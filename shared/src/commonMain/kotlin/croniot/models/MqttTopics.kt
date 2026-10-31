package croniot.models

object MqttTopics {
    fun newTasks(deviceUuid: String) = "/$deviceUuid/newTasks"
    fun sensorData(deviceUuid: String) = "/server_to_app/$deviceUuid/sensor_data"
    fun taskProgressWildcard(deviceUuid: String) = "/server_to_devices/$deviceUuid/task_types/+/tasks/+/progress"
    fun taskProgress(deviceUuid: String, taskTypeUid: Long, taskUid: Long) =
        "/server_to_devices/$deviceUuid/task_types/$taskTypeUid/tasks/$taskUid/progress"

    fun featureFlagUpdate(flagName: String) = "/server/feature_flags/$flagName"
    const val FEATURE_FLAG_UPDATES_WILDCARD = "/server/feature_flags/+"

    // Retained firmware update request for one device (croniot-iot ota/OtaManifest.h).
    fun otaRequest(deviceUuid: String) = "/server/$deviceUuid/ota"
}