package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.SensorDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SensorDataEntity)

    @Query(
        """
        SELECT * FROM sensor_data
        WHERE deviceUuid = :deviceUuid AND sensorTypeUid = :sensorTypeUid
        ORDER BY timeStampMillis DESC
        LIMIT :limit
    """
    )
    suspend fun getLatest(deviceUuid: String, sensorTypeUid: Long, limit: Int): List<SensorDataEntity>

    @Query(
        """
        SELECT * FROM sensor_data
        WHERE deviceUuid = :deviceUuid AND sensorTypeUid = :sensorTypeUid
        ORDER BY timeStampMillis DESC
        LIMIT 1
    """
    )
    fun observeLatest(deviceUuid: String, sensorTypeUid: Long): Flow<SensorDataEntity?>
}
