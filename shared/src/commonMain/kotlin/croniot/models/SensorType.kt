package croniot.models

data class SensorType(
    val uid: Long,
    val name: String,
    val description: String,
    val parameters: List<ParameterSensor>,
)