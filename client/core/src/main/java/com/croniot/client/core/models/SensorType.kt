package com.croniot.client.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class SensorType(
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: Set<ParameterSensor>,
) : Parcelable

fun SensorType.isChartable(): Boolean {
    val firstParam = parameters.firstOrNull() ?: return false
    val max = firstParam.constraints["maxValue"]?.toFloat()
    val min = firstParam.constraints["minValue"]?.toFloat()
    return max != null && min != null
}
