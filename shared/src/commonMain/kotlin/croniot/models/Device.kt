package croniot.models

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val uuid: String,
    val name: String,
    val description: String = "",
    val iot: Boolean,
    val sensorTypes: List<SensorType> = emptyList(),
    val taskTypes: List<TaskType> = emptyList(),
)
