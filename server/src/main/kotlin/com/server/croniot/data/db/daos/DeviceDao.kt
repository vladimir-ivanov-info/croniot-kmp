package com.croniot.server.db.daos

import croniot.models.Device

interface DeviceDao {

    fun insert(device: Device) : Long

    fun getAll() : List<Device>

    fun getByUuid(deviceUuid: String) : Device?

    fun getLazy(deviceUuid: String) : Device?

}