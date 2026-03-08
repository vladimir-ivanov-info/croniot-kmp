package com.croniot.client.data.source.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaskEntity): Long

    @Query("SELECT * FROM tasks WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deviceId = :deviceId")
    suspend fun getByDeviceId(deviceId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE taskTypeId = :taskTypeId")
    suspend fun getByTaskTypeId(taskTypeId: Long): List<TaskEntity>
}
