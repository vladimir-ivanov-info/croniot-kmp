package croniot.models.dto

data class TaskDto(
    var deviceUuid: String = "",
    var taskTypeUid: Long = 0, // TODO change to taskTypeUid
    var uid: Long = 0,
    var parametersValues: MutableMap<Long, String> = mutableMapOf(), // key,value = ParameterTask.uid,value
    var stateInfos: MutableSet<TaskStateInfoDto> = mutableSetOf(),
    // var progress: Double = 0.0
) {

    fun getLastState(): TaskStateInfoDto {
        return stateInfos.toList().sortedByDescending { it.dateTime }.first() // TODO stateinfos should never be empty, at least one state must exist
    }
}
