package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceTokenDao
import croniot.models.Device
import croniot.models.DeviceToken
import javax.inject.Inject

class DeviceTokenRepository @Inject constructor(
    private val deviceTokenDao: DeviceTokenDao
){

    fun createDeviceToken(device: Device, token: String){

        val deviceToken = DeviceToken(device, token)
        deviceTokenDao.insert(deviceToken)
    }

    fun getDeviceAssociatedWithToken(token: String) : Device? {
        return deviceTokenDao.getDeviceAssociatedWithToken(token)
    }

}