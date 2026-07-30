package com.croniot.client.data.source.remote.mappers

import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.ParameterSensorDto
import croniot.models.dto.ParameterTaskDto
import croniot.models.dto.SensorDataDto
import croniot.models.dto.SensorTypeDto
import croniot.models.dto.TaskTypeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class DtoToDomainMapperTest {

    private val parameterTaskDto = ParameterTaskDto(
        uid = 1L,
        name = "power",
        type = "STATEFUL",
        unit = "",
        description = "Power switch",
        constraints = mapOf("state_1" to "ON", "state_2" to "OFF"),
    )

    private val parameterSensorDto = ParameterSensorDto(
        uid = 2L,
        name = "temperature",
        type = "NUMBER",
        unit = "C",
        description = "Temperature sensor",
        constraints = mapOf("minValue" to "0", "maxValue" to "100"),
    )

    @Test
    fun `ParameterTaskDto maps all fields to domain`() {
        val result = parameterTaskDto.toDomain()

        assertEquals(1L, result.uid)
        assertEquals("power", result.name)
        assertEquals("STATEFUL", result.type)
        assertEquals("", result.unit)
        assertEquals("Power switch", result.description)
        assertEquals(mapOf("state_1" to "ON", "state_2" to "OFF"), result.constraints)
    }

    @Test
    fun `ParameterSensorDto maps all fields to domain`() {
        val result = parameterSensorDto.toDomain()

        assertEquals(2L, result.uid)
        assertEquals("temperature", result.name)
        assertEquals("NUMBER", result.type)
        assertEquals("C", result.unit)
        assertEquals("Temperature sensor", result.description)
        assertEquals(mapOf("minValue" to "0", "maxValue" to "100"), result.constraints)
    }

    @Test
    fun `TaskTypeDto maps fields and nested parameters`() {
        val dto = TaskTypeDto(
            uid = 10L,
            name = "Turn on",
            description = "Turns the device on",
            parameters = listOf(parameterTaskDto),
        )

        val result = dto.toDomain()

        assertEquals(10L, result.uid)
        assertEquals("Turn on", result.name)
        assertEquals("Turns the device on", result.description)
        assertEquals(1, result.parameters.size)
        assertEquals(parameterTaskDto.uid, result.parameters.first().uid)
    }

    @Test
    fun `SensorTypeDto maps fields and nested parameters`() {
        val dto = SensorTypeDto(
            uid = 20L,
            name = "Temperature",
            description = "Ambient temperature",
            parameters = listOf(parameterSensorDto),
        )

        val result = dto.toDomain()

        assertEquals(20L, result.uid)
        assertEquals("Temperature", result.name)
        assertEquals("Ambient temperature", result.description)
        assertEquals(1, result.parameters.size)
        assertEquals(parameterSensorDto.uid, result.parameters.first().uid)
    }

    @Test
    fun `DeviceDto maps fields and nested sensor and task types`() {
        val dto = DeviceDto(
            uuid = "device-1",
            name = "Living room",
            description = "Main sensor",
            iot = true,
            sensorTypes = listOf(SensorTypeDto(20L, "Temperature", "desc", listOf(parameterSensorDto))),
            taskTypes = listOf(TaskTypeDto(10L, "Turn on", "desc", listOf(parameterTaskDto))),
        )

        val result = dto.toDomain()

        assertEquals("device-1", result.uuid)
        assertEquals("Living room", result.name)
        assertEquals("Main sensor", result.description)
        assertEquals(1, result.sensorTypes.size)
        assertEquals(1, result.taskTypes.size)
        assertEquals(20L, result.sensorTypes.first().uid)
        assertEquals(10L, result.taskTypes.first().uid)
    }

    @Test
    fun `AccountDto maps fields and nested devices`() {
        val deviceDto = DeviceDto(
            uuid = "device-1",
            name = "Living room",
            description = "",
            iot = true,
            sensorTypes = emptyList(),
            taskTypes = emptyList(),
        )
        val dto = AccountDto(
            uuid = "account-1",
            nickname = "nick",
            email = "user@example.com",
            devices = listOf(deviceDto),
        )

        val result = dto.toDomain()

        assertEquals("account-1", result.uuid)
        assertEquals("nick", result.nickname)
        assertEquals("user@example.com", result.email)
        assertEquals(1, result.devices.size)
        assertEquals("device-1", result.devices.first().uuid)
    }

    @Test
    fun `SensorDataDto maps fields including renamed timestamp property`() {
        val now = ZonedDateTime.now()
        val dto = SensorDataDto(
            deviceUuid = "device-1",
            sensorTypeUid = 5L,
            value = "23.5",
            timestamp = now,
        )

        val result = dto.toDomain()

        assertEquals("device-1", result.deviceUuid)
        assertEquals(5L, result.sensorTypeUid)
        assertEquals("23.5", result.value)
        assertEquals(now, result.timeStamp)
    }
}
