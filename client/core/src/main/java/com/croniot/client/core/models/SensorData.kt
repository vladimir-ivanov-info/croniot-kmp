package com.croniot.client.core.models

import java.time.ZonedDateTime

data class SensorData(
    var deviceUuid: String,
    var sensorTypeUid: Long,
    var value: String,
    var timeStamp: ZonedDateTime,
)
