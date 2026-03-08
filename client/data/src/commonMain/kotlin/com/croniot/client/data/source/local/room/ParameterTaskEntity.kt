package com.croniot.client.data.source.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parameter_task",
    foreignKeys = [
        ForeignKey(
            entity = TaskTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["uid"], unique = true),
        Index("taskTypeId"),
    ],
)
data class ParameterTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: Long,
    val taskTypeId: Long,
    val name: String,
    val type: String,
    val unit: String,
    val description: String,
    val constraintsJson: String = "{}",
)
