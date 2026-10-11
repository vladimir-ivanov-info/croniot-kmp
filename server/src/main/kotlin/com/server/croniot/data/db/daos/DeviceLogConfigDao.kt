package com.server.croniot.data.db.daos

interface DeviceLogConfigDao {
    fun upsert(deviceId: Long, configJson: String)
    fun get(deviceId: Long): String?
}
