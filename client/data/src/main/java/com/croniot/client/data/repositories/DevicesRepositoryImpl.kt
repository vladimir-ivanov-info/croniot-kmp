package com.croniot.client.data.repositories

import com.croniot.client.core.models.Device

class DevicesRepositoryImpl : DevicesRepository {

    override fun getDevices(accountUuid: String): List<Device> {
        return emptyList() //TODO
    }
}