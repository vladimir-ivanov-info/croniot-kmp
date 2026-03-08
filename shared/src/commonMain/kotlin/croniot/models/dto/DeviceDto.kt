package croniot.models.dto

data class DeviceDto(
    val uuid: String,
    val name: String,
    val description: String,
    val iot: Boolean,
    val sensorTypes: List<SensorTypeDto>,
    val taskTypes: List<TaskTypeDto>,
)