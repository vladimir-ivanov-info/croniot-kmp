package com.croniot.server.db.daos

import croniot.models.Device
import croniot.models.DeviceToken

interface DeviceTokenDao {

    fun insert(deviceToken: DeviceToken)

    fun getDeviceAssociatedWithToken(token: String): Device?

}