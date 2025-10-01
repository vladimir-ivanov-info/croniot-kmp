package croniot.models.dto

data class DeviceDto(
    var uuid: String,
    var name: String,
    var description: String,
    var sensorTypes: List<SensorTypeDto>,
    var taskTypes: List<TaskTypeDto>, // TODO rename to "taskTypes"
    //var lastActiveTimestamp: ZonedDateTime,
)
