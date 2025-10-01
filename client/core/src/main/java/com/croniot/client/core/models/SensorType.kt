package com.croniot.client.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SensorType(
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: Set<ParameterSensor>
) : Parcelable
