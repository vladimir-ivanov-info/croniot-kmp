package com.croniot.client.core.models

import android.os.Parcelable
import croniot.models.ParameterTypes
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

@Parcelize
data class ParameterTask(
    var uid: Long,
    var name: String,
    var type: String,
    var unit: String,
    var description: String,
    var constraints: Map<String, String>,
) : Parcelable

fun ParameterTask.isStateful(): Boolean {
    return this.type == ParameterTypes.STATEFUL
}

fun ParameterTask.isRepresentsSwitch(): Boolean {
    return this.constraints.size == 2 &&
            this.constraints.containsKey("state_1") &&
            this.constraints.containsKey("state_2")
}

fun ParameterTask.isRepresentsSlider(): Boolean {
    return this.constraints.containsKey("minValue") &&
            this.constraints.containsKey("maxValue") &&
            this.constraints.containsKey("stepSize")
}

fun ParameterTask.formatValue(value: Float): String {
    val decimals = this.constraints["decimals"]
    if (decimals != null && decimals.toInt() == 0) {
        return value.roundToInt().toString()
    }
    return value.toString()
}
