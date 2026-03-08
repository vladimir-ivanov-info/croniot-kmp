package com.croniot.client.data.source.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_data",
    indices = [
        Index(value = ["deviceUuid", "sensorTypeUid"]),
    ],
)
data class SensorDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceUuid: String,
    val sensorTypeUid: Long,
    val value: String,
    val timeStampMillis: Long,
)
