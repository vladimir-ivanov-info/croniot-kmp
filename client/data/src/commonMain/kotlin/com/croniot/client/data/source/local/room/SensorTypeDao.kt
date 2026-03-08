package com.croniot.client.data.source.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SensorTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SensorTypeEntity): Long

    @Query("SELECT * FROM sensor_types WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: Long): SensorTypeEntity?

    @Query("SELECT * FROM sensor_types WHERE deviceId = :deviceId")
    suspend fun getByDeviceId(deviceId: Long): List<SensorTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParameter(entity: ParameterSensorEntity): Long

    @Query("SELECT * FROM parameter_sensor WHERE sensorTypeId = :sensorTypeId")
    suspend fun getParametersBySensorTypeId(sensorTypeId: Long): List<ParameterSensorEntity>
}
