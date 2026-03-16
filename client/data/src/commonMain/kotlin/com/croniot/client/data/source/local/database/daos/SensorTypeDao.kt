package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.SensorTypeEntity

@Dao
interface SensorTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SensorTypeEntity): Long

    @Query("SELECT * FROM sensor_types WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: Long): SensorTypeEntity?

    @Query("SELECT * FROM sensor_types WHERE deviceId = :deviceId")
    suspend fun getByDeviceId(deviceId: Long): List<SensorTypeEntity>
}
