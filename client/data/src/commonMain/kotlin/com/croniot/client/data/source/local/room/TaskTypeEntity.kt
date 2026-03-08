package com.croniot.client.data.source.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_types",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uid"], unique = true),
        Index("deviceId"),
    ],
)
data class TaskTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: Long,
    val deviceId: Long,
    val name: String,
    val description: String,
    val realTime: Boolean = false,
)
