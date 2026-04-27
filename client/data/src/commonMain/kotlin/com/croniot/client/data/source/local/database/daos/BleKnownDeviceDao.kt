package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.BleKnownDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BleKnownDeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BleKnownDeviceEntity)

    @Query("SELECT * FROM ble_known_devices ORDER BY lastSeenAtMillis DESC")
    fun observeAll(): Flow<List<BleKnownDeviceEntity>>

    @Query("SELECT * FROM ble_known_devices WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): BleKnownDeviceEntity?

    @Query("SELECT uuid FROM ble_known_devices")
    suspend fun getAllUuids(): List<String>

    @Query("UPDATE ble_known_devices SET lastSeenAtMillis = :timestampMillis WHERE uuid = :uuid")
    suspend fun touchLastSeen(uuid: String, timestampMillis: Long)

    @Query("DELETE FROM ble_known_devices WHERE uuid = :uuid")
    suspend fun delete(uuid: String)
}
