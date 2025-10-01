package com.croniot.client.core.models

import croniot.models.TaskState


data class Task(
    var deviceUuid: String = "",
    var taskTypeUid: Long = 0, // TODO change to taskTypeUid
    var uid: Long = 0,
    var parametersValues: MutableMap<Long, String> = mutableMapOf(), // key,value = ParameterTask.uid,value
    var stateInfos: MutableSet<TaskStateInfo> = mutableSetOf(),
) {

    fun getMostRecentState(): TaskStateInfo {
        return stateInfos.toList().sortedByDescending { it.dateTime }.first() // TODO stateinfos should never be empty, at least one state must exist
    }

    fun getMostRecentStateEmittedByIoT(): TaskStateInfo? {
        //TODO
        return stateInfos.toList().filter{
            it.state != TaskState.CREATED
            && it.state != TaskState.UNDEFINED
            && it.state != TaskState.ERROR //TODO we have to differentiate between ERROR assigned by server and ERROR assigned by IoT. For now we leave it generic.
        }.maxByOrNull { it.dateTime } // TODO stateinfos should never be empty, at least one state must exist
    }
}
