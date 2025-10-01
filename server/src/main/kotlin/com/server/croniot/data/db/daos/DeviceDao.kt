package com.server.croniot.data.db.daos

import croniot.models.Account
import croniot.models.Device

interface DeviceDao {

    fun insert(account: Account, device: Device) // : Long

    fun getAll(): List<Device>

    fun getByUuid(deviceUuid: String): Device?

    fun getLazy(deviceUuid: String): Device?
}
