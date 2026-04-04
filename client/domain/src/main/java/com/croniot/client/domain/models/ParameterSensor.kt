package com.croniot.client.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ParameterSensor(
    var uid: Long,
    var name: String,
    var type: String,
    var unit: String,
    var description: String,
    var constraints: Map<String, String>,
)
