package com.server.croniot.testsupport.fakes

import com.server.croniot.data.db.daos.DeviceDao
import com.server.croniot.data.db.entities.DeviceEntity
import com.server.croniot.data.mappers.toDomain
import croniot.models.Device

class FakeDeviceDao : DeviceDao {

    private val byId = mutableMapOf<Long, DeviceEntity>()
    private var nextId = 1L

    fun seed(entity: DeviceEntity): Long {
        val id = if (entity.id != 0L) entity.id else nextId++
        byId[id] = entity.copy(id = id)
        return id
    }

    override fun getDevices(accountId: Long): List<DeviceEntity> = byId.values.filter { it.accountId == accountId }

    override fun insert(device: DeviceEntity): Long = seed(device)

    override fun upsert(device: DeviceEntity): Long {
        val existing = byId.values.firstOrNull { it.uuid == device.uuid }
        val id = existing?.id ?: (if (device.id != 0L) device.id else nextId++)
        byId[id] = device.copy(id = id)
        return id
    }

    override fun getAll(): List<Device> = byId.values.map { it.toDomain() }

    override fun getByUuid(deviceUuid: String): Device? = byId.values.firstOrNull { it.uuid == deviceUuid }?.toDomain()

    override fun getLazy(deviceUuid: String): Device? = getByUuid(deviceUuid)

    override fun getDeviceId(deviceUuid: String): Long? = byId.values.firstOrNull { it.uuid == deviceUuid }?.id

    override fun isDeviceExists(deviceUuid: String): Boolean = byId.values.any { it.uuid == deviceUuid }
}
