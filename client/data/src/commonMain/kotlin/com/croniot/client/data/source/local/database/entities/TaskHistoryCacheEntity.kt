package com.croniot.client.data.source.local.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_history_cache",
    indices = [
        Index("deviceUuid"),
        Index(value = ["deviceUuid", "timeStampMillis"]),
        Index(value = ["deviceUuid", "stateInfoId"], unique = true),
    ],
)
data class TaskHistoryCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceUuid: String,
    val stateInfoId: Long?,
    val taskUid: Long,
    val taskTypeUid: Long,
    val timeStampMillis: Long,
    val state: String,
    val progress: Double,
    val errorMessage: String,
)
