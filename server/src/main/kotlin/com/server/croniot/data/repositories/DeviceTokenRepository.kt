package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceTokenDao
import croniot.models.Device
import croniot.models.DeviceToken
import javax.inject.Inject

class DeviceTokenRepository @Inject constructor(
    private val deviceTokenDao: DeviceTokenDao,
) {

    //fun createDeviceToken(device: Device, token: String) {
    fun createDeviceToken(deviceId: Long, token: String) {
        val deviceToken = DeviceToken(deviceId, token)
        deviceTokenDao.insert(deviceToken)
    }

    fun getDevice(token: String): Device? {
        return deviceTokenDao.getDeviceAssociatedWithToken(token)
    }

    fun getDeviceUuid(token: String): String? {
        return deviceTokenDao.getDeviceUuidAssociatedWithToken(token)
    }

    fun isTokenCorrect(deviceUuid: String, token: String): Boolean {
        return deviceTokenDao.isTokenCorrect(deviceUuid, token)
    }
}
