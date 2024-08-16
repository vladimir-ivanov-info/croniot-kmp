package croniot.models.dto

data class TaskDto(
    var deviceUuid: String = "",
    var taskUid: Long = 0,
    var uid: Long = 0,
    var parametersValues: MutableMap<Long, String> = mutableMapOf(), //key,value = ParameterTask.uid,value
    var stateInfos: MutableSet<TaskStateInfoDto> = mutableSetOf()
) {

    fun getLastState() : TaskStateInfoDto {
        return stateInfos.toList().sortedByDescending { it.dateTime }.first()
    }

}
