package com.server.croniot.data.db.daos

import com.server.croniot.data.db.entities.DeviceEntity
import croniot.models.Device

interface DeviceDao {

    fun getDevices(accountId: Long): List<DeviceEntity>

    // fun insert(account: Account, device: Device) // : Long
    fun insert(device: DeviceEntity): Long

    fun upsert(device: DeviceEntity): Long

    fun getAll(): List<Device>

    fun getByUuid(deviceUuid: String): Device?

    fun getLazy(deviceUuid: String): Device?

    fun getDeviceId(deviceUuid: String): Long?

    fun isDeviceExists(deviceUuid: String): Boolean
}
