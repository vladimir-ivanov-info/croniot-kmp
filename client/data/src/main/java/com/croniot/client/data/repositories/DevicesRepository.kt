package com.croniot.client.data.repositories

import com.croniot.client.core.models.Device

interface DevicesRepository {

    fun getDevices(accountUuid: String): List<Device>
}
