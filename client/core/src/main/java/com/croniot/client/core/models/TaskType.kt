package com.croniot.client.core.models

import android.os.Parcelable
import croniot.models.ParameterTypes
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TaskType(
    var uid: Long = 0,
    var name: String,
    var description: String,
    var parameters: List<ParameterTask>,
) : Parcelable

fun TaskType.isInstant(): Boolean =
    parameters.size == 1 && parameters.none { it.type == ParameterTypes.TIME }
