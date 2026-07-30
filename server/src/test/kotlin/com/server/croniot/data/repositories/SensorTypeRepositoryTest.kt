package com.server.croniot.data.repositories

import com.server.croniot.testsupport.fakes.FakeSensorTypeDao
import croniot.models.SensorType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit test for [SensorTypeRepository] using a fake (not a mock) of
 * [com.server.croniot.data.db.daos.SensorTypeDao]. This repository has a single method, a plain
 * delegation; it is still covered against the fake's realistic write-then-read behavior.
 */
class SensorTypeRepositoryTest {

    private val sensorTypeDao = FakeSensorTypeDao()
    private val repository = SensorTypeRepository(sensorTypeDao)

    @Test
    fun `upsert persists the sensor type so it becomes retrievable by device id`() {
        val sensorType = SensorType(uid = 1L, name = "Temp", description = "", parameters = emptyList())

        repository.upsert(sensorType, deviceId = 42L)

        assertEquals(listOf(sensorType), sensorTypeDao.getByDeviceIds(listOf(42L))[42L])
    }
}
