package com.croniot.client.data.repositories

import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.DevicesRepository

class DevicesRepositoryImpl : DevicesRepository {

    override fun getDevices(accountUuid: String): List<Device> {
        return emptyList() // TODO
    }
}
