package com.croniot.client.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Device(
    val uuid: String,
    val name: String,
    val description: String,
    val sensorTypes: List<SensorType> = emptyList(),
    val taskTypes: List<TaskType> = emptyList(),
) : Parcelable
