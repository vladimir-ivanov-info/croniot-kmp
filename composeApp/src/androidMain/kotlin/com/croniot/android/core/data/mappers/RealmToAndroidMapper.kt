package com.croniot.android.core.data.mappers

import com.croniot.android.core.data.entities.SensorDataRealm
import com.croniot.android.domain.model.SensorData
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun SensorDataRealm.toAndroidModel(): SensorData {
    return SensorData(
        deviceUuid = this.deviceUuid,
        sensorTypeUid = this.sensorTypeUid,
        value = this.value,
        dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.timestampMillis), ZoneOffset.UTC),
    )
}
