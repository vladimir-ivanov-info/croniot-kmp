package com.croniot.android.domain.model

import java.time.ZonedDateTime

data class SensorData(
    // var id: Long = 0,
    var deviceUuid: String,
    var sensorTypeUid: Long,
    var value: String,
    var dateTime: ZonedDateTime,
)
