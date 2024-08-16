package croniot.models.dto

data class DeviceDto(
    var uuid: String,
    var name: String,
    var description: String,
    var sensors: MutableSet<SensorDto>,
    var tasks: MutableSet<TaskTypeDto>,
    var lastOnlineMillis: Long
)