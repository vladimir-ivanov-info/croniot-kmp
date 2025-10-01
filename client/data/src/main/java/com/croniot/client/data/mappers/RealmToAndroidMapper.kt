package com.croniot.client.data.mappers

import com.croniot.android.core.data.entities.SensorDataRealm
import com.croniot.android.core.data.entities.TaskStateInfoRealm
import com.croniot.client.core.models.SensorData
import com.croniot.client.core.models.TaskStateInfo
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun SensorDataRealm.toAndroidModel(): SensorData {
    return SensorData(
        deviceUuid = this.deviceUuid,
        sensorTypeUid = this.sensorTypeUid,
        value = this.value,
        timeStamp = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(this.timeStampMillis),
            ZoneOffset.UTC,
        ),
    )
}

fun TaskStateInfoRealm.toAndroidModel(): TaskStateInfo {
    return TaskStateInfo(
        deviceUuid = this.deviceUuid,
        taskTypeUid = this.taskTypeUid,
        taskUid = this.taskUid,
        dateTime = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(this.timeStampMillis),
            ZoneOffset.UTC,
        ),
        state = this.state,
        progress = this.progress,
        errorMessage = this.errorMessage,
    )
}
