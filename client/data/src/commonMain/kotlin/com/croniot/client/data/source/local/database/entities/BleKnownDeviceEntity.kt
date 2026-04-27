package com.croniot.client.data.source.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ble_known_devices")
data class BleKnownDeviceEntity(
    @PrimaryKey val uuid: String,
    val displayName: String,
    val macAddress: String,
    val lastSeenAtMillis: Long,
    val addedAtMillis: Long,
)
