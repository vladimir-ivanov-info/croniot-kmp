package croniot.models.dto

data class ParameterDto(
    var uid: Long,
    var name: String,
    var type: String,
    var unit: String,
    var description: String,
    var constraints: MutableMap<String, String>,
)
