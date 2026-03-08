package com.croniot.client.domain.repositories

import com.croniot.client.core.models.Device

interface DevicesRepository {

    fun getDevices(accountUuid: String): List<Device>
}
