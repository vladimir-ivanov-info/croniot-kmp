package com.croniot.android.domain.model

data class Device(
    val uuid: String,
    val name: String,
    val description: String,
    val sensors: MutableSet<SensorType>,
    val taskTypes: MutableSet<TaskType>,
){
    override fun hashCode(): Int {
        return uuid.hashCode() // Ensure `uuid` is always non-null
    }

    /*override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Device

        return uuid == other.uuid
    }*/

    /*fun getLastOnlineZonedDateTime() : ZonedDateTime? {

        val result : ZonedDateTime? = null

        for(sensor in sensors){

        }

        sensors.sortedBy { it. }

    }*/
}