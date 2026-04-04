package com.croniot.client.domain.models

import java.time.ZonedDateTime

data class SensorData(
    var deviceUuid: String,
    var sensorTypeUid: Long,
    var value: String,
    var timeStamp: ZonedDateTime,
)
