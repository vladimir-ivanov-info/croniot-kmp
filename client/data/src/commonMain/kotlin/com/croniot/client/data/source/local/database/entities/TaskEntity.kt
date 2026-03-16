package com.croniot.client.data.source.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("deviceId"),
        Index("taskTypeId"),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: Long,
    val deviceId: Long,
    val taskTypeId: Long,
    val parametersValuesJson: String = "{}",
)
