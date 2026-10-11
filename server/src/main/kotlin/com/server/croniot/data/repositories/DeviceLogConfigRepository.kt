package com.server.croniot.data.repositories

import com.server.croniot.data.db.daos.DeviceLogConfigDao
import javax.inject.Inject

class DeviceLogConfigRepository @Inject constructor(
    private val deviceLogConfigDao: DeviceLogConfigDao,
) {
    fun upsert(deviceId: Long, configJson: String) {
        deviceLogConfigDao.upsert(deviceId, configJson)
    }

    fun get(deviceId: Long): String? {
        return deviceLogConfigDao.get(deviceId)
    }
}
