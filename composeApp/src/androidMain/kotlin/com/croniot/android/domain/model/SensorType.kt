package com.croniot.android.domain.model

data class SensorType(
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterSensor>,
)
