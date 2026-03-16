package croniot.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val uuid: String,
    val name: String,
    val description: String,
    val iot: Boolean,
    val sensorTypes: List<SensorTypeDto>,
    val taskTypes: List<TaskTypeDto>,
)
