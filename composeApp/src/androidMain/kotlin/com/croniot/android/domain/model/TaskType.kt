package com.croniot.android.domain.model

data class TaskType(
    var id: Long = 0,
    var uid: Long = 0,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterTask>,
    var realTime: Boolean,
    // var continuous: Boolean
    // var stateful: Boolean
)
