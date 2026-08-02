package com.croniot.client.data.source.local.database.entities

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EntitiesTest {

    @Test
    fun `WHEN AccountEntity is constructed without an id THEN it defaults to zero and copy overrides it`() {
        val entity = AccountEntity(uuid = "acc-1", nickname = "nick", email = "user@example.com")

        assertEquals(0L, entity.id)
        assertEquals(5L, entity.copy(id = 5L).id)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(nickname = "other"))
        assertTrue(entity.toString().contains("acc-1"))
        assertEquals(entity.hashCode(), entity.copy().hashCode())
    }

    @Test
    fun `WHEN BleKnownDeviceEntity is constructed without schema fields THEN schemaVersion and schemaJson use their defaults`() {
        val entity = BleKnownDeviceEntity(
            uuid = "device-1",
            displayName = "Device",
            macAddress = "AA:BB:CC:DD:EE:FF",
            lastSeenAtMillis = 1000L,
            addedAtMillis = 500L,
        )

        assertEquals(0L, entity.schemaVersion)
        assertEquals(null, entity.schemaJson)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(schemaVersion = 2L))
        assertTrue(entity.toString().contains("device-1"))
    }

    @Test
    fun `WHEN DeviceEntity is constructed without id and lastOnlineMillis THEN both default to zero`() {
        val entity = DeviceEntity(uuid = "device-1", accountId = 1L, name = "Device", description = "desc")

        assertEquals(0L, entity.id)
        assertEquals(0L, entity.lastOnlineMillis)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(name = "other"))
    }

    @Test
    fun `WHEN ParameterSensorEntity is constructed without constraintsJson THEN it defaults to an empty object`() {
        val entity = ParameterSensorEntity(
            uid = 1L,
            sensorTypeId = 1L,
            name = "temp",
            type = "NUMBER",
            unit = "C",
            description = "desc",
        )

        assertEquals("{}", entity.constraintsJson)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(uid = 2L))
    }

    @Test
    fun `WHEN ParameterTaskEntity is constructed without constraintsJson THEN it defaults to an empty object`() {
        val entity = ParameterTaskEntity(
            uid = 1L,
            taskTypeId = 1L,
            name = "duration",
            type = "NUMBER",
            unit = "s",
            description = "desc",
        )

        assertEquals("{}", entity.constraintsJson)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(name = "other"))
    }

    @Test
    fun `WHEN SensorDataEntity is constructed THEN it holds device uuid sensor type and value`() {
        val entity = SensorDataEntity(deviceUuid = "device-1", sensorTypeUid = 10L, value = "25.5", timeStampMillis = 1000L)

        assertEquals(0L, entity.id)
        assertEquals("25.5", entity.value)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(value = "26.0"))
    }

    @Test
    fun `WHEN SensorTypeEntity is constructed THEN it holds device id name and description`() {
        val entity = SensorTypeEntity(uid = 1L, deviceId = 1L, name = "Temperature", description = "desc")

        assertEquals(0L, entity.id)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(name = "other"))
    }

    @Test
    fun `WHEN TaskEntity is constructed without parametersValuesJson THEN it defaults to an empty object`() {
        val entity = TaskEntity(uid = 1L, deviceId = 1L, taskTypeId = 1L)

        assertEquals("{}", entity.parametersValuesJson)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(uid = 2L))
    }

    @Test
    fun `WHEN TaskHistoryCacheEntity is constructed with a null stateInfoId THEN it holds it as null`() {
        val entity = TaskHistoryCacheEntity(
            deviceUuid = "device-1",
            stateInfoId = null,
            taskUid = 1L,
            taskTypeUid = 10L,
            timeStampMillis = 1000L,
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )

        assertEquals(null, entity.stateInfoId)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(stateInfoId = 1L))
    }

    @Test
    fun `WHEN TaskStateInfoEntity is constructed THEN it holds task id state and progress`() {
        val entity = TaskStateInfoEntity(
            taskId = 1L,
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
            timeStampMillis = 1000L,
        )

        assertEquals(0L, entity.id)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(progress = 0.75))
    }

    @Test
    fun `WHEN TaskTypeEntity is constructed without realTime THEN it defaults to false`() {
        val entity = TaskTypeEntity(uid = 1L, deviceId = 1L, name = "Water", description = "desc")

        assertEquals(false, entity.realTime)
        assertEquals(entity, entity.copy())
        assertNotEquals(entity, entity.copy(realTime = true))
    }
}
