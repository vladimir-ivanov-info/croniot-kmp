package com.croniot.client.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParameterSensor(
    var uid: Long,
    var name: String,
    var type: String,
    var unit: String,
    var description: String,
    var constraints: Map<String, String>,
) : Parcelable
