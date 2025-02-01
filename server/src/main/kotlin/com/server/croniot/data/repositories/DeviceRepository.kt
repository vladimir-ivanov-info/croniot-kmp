package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceDao
import croniot.models.Device
import javax.inject.Inject

class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao
) {

    fun getByUuid(deviceUuid: String) : Device? {
        return deviceDao.getByUuid(deviceUuid)
    }

    fun getAll() : List<Device> {
        return deviceDao.getAll()
    }

    fun createDevice(device: Device){
        deviceDao.insert(device)
    }

    fun getLazy(deviceUuid: String) : Device? {
        return deviceDao.getLazy(deviceUuid)
    }

}