package croniot.models.dto

import java.time.ZonedDateTime

data class DeviceDto(
    var uuid: String,
    var name: String,
    var description: String,
    var sensors: MutableSet<SensorTypeDto>,
    var tasks: MutableSet<TaskTypeDto>, // TODO rename to "taskTypes"
    //var lastActiveTimestamp: ZonedDateTime,
)
