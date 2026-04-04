package com.croniot.client.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val uuid: String,
    val name: String,
    val description: String,
    val sensorTypes: List<SensorType> = emptyList(),
    val taskTypes: List<TaskType> = emptyList(),
)
