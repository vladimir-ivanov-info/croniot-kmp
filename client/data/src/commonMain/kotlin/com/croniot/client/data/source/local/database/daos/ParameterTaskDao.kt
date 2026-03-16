package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.ParameterTaskEntity

@Dao
interface ParameterTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ParameterTaskEntity): Long

    @Query("SELECT * FROM parameter_task WHERE taskTypeId = :taskTypeId")
    suspend fun getByTaskTypeId(taskTypeId: Long): List<ParameterTaskEntity>
}
