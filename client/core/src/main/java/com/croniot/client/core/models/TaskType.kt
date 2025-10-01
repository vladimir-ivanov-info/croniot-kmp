package com.croniot.client.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskType(
    var uid: Long = 0,
    var name: String,
    var description: String,
    var parameters: /*Mutable*/Set<ParameterTask>,
    var realTime: Boolean,
) : Parcelable
