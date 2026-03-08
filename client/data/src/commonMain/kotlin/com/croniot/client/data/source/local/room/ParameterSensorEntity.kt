package com.croniot.client.data.source.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parameter_sensor",
    foreignKeys = [
        ForeignKey(
            entity = SensorTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["sensorTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uid"], unique = true),
        Index("sensorTypeId"),
    ],
)
data class ParameterSensorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: Long,
    val sensorTypeId: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,
    val constraintsJson: String = "{}",
)
