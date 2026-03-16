package com.croniot.client.data.source.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.croniot.client.data.source.local.database.entities.DeviceEntity

@Dao
interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeviceEntity): Long

    @Query("SELECT * FROM devices WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE accountId = :accountId")
    suspend fun getByAccountId(accountId: Long): List<DeviceEntity>

    @Query("DELETE FROM devices WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}
