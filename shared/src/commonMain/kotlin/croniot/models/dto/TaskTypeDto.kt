package croniot.models.dto

data class TaskTypeDto(
    var id: Long = 0,
    var uid: Long = 0,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterTaskDto>,
    var realTime: Boolean,
    //var continuous: Boolean
    //var stateful: Boolean
)
