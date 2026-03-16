package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.ParameterSensorEntity

@Dao
interface ParameterSensorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ParameterSensorEntity): Long

    @Query("SELECT * FROM parameter_sensor WHERE sensorTypeId = :sensorTypeId")
    suspend fun getBySensorTypeId(sensorTypeId: Long): List<ParameterSensorEntity>
}
