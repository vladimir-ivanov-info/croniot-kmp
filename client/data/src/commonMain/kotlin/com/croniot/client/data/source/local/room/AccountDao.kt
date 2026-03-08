package com.croniot.client.data.source.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountEntity): Long

    @Query("SELECT * FROM accounts WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Query("DELETE FROM accounts WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}
