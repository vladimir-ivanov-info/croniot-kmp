package com.croniot.client.data.source.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskStateInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaskStateInfoEntity): Long

    @Query("SELECT * FROM task_state_info WHERE taskId = :taskId ORDER BY timeStampMillis DESC LIMIT 1")
    fun observeLatestByTaskId(taskId: Long): Flow<TaskStateInfoEntity?>

    @Query("SELECT * FROM task_state_info WHERE taskId = :taskId ORDER BY timeStampMillis DESC")
    suspend fun getByTaskId(taskId: Long): List<TaskStateInfoEntity>
}
