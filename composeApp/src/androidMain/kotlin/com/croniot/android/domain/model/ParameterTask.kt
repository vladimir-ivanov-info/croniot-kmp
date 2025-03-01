package com.croniot.android.domain.model

data class ParameterTask(
    var uid: Long,
    var name: String,
    var type: String,
    var unit: String,
    var description: String,
    var constraints: MutableMap<String, String>,
)
