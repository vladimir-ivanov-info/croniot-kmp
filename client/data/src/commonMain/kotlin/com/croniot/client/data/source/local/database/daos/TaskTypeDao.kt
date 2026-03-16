package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.TaskTypeEntity

@Dao
interface TaskTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaskTypeEntity): Long

    @Query("SELECT * FROM task_types WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: Long): TaskTypeEntity?

    @Query("SELECT * FROM task_types WHERE deviceId = :deviceId")
    suspend fun getByDeviceId(deviceId: Long): List<TaskTypeEntity>
}
