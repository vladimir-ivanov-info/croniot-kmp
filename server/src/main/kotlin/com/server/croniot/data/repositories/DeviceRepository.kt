package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceDao
import com.server.croniot.data.mappers.toEntity
import croniot.models.Device
import javax.inject.Inject

class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao,
) {

    fun getId(deviceUuid: String) : Long? {
        return deviceDao.getDeviceId(deviceUuid)
    }

    fun isDeviceExists(deviceUuid: String) : Boolean {
        return deviceDao.isDeviceExists(deviceUuid)
    }


    fun getByUuid(deviceUuid: String): Device? {
        return deviceDao.getByUuid(deviceUuid)
    }

    fun getAll(): List<Device> {
        return deviceDao.getAll()
    }

    fun createDevice(device: Device, accountId: Long) : Long {
        val device = device.toEntity(accountId)
       // return deviceDao.insert(device)
        return deviceDao.upsert(device)
        //println()
    }

    fun getLazy(deviceUuid: String): Device? {
        return deviceDao.getLazy(deviceUuid)
    }
}
