package com.croniot.client.domain.models

import croniot.models.ParameterTypes
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

fun ParameterSensor.isNumeric(): Boolean = type == ParameterTypes.NUMBER

fun ParameterSensor.isTime(): Boolean = type == ParameterTypes.TIME

fun ParameterSensor.isStateful(): Boolean = type == ParameterTypes.STATEFUL