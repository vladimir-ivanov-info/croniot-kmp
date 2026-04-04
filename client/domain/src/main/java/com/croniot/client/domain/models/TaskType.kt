package com.croniot.client.domain.models

import croniot.models.ParameterTypes
import kotlinx.serialization.Serializable

@Serializable
data class TaskType(
    var uid: Long = 0,
    var name: String,
    var description: String,
    var parameters: List<ParameterTask>,
)

fun TaskType.isInstant(): Boolean =
    parameters.size == 1 && parameters.none { it.type == ParameterTypes.TIME }
