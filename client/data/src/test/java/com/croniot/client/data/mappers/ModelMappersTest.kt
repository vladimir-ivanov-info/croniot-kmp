package com.croniot.client.data.mappers

import croniot.models.dto.TaskDto
import croniot.models.dto.TaskStateInfoDto
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ModelMappersTest {

    private val stateInfoDto = TaskStateInfoDto(
        dateTime = ZonedDateTime.parse("2026-01-01T10:00:00Z"),
        state = "DONE",
        progress = 1.0,
        errorMessage = "",
    )

    @Test
    fun `TaskStateInfoDto maps all fields to domain`() {
        val result = stateInfoDto.toModel()

        assertEquals(stateInfoDto.dateTime, result.dateTime)
        assertEquals("DONE", result.state)
        assertEquals(1.0, result.progress)
        assertEquals("", result.errorMessage)
    }

    @Test
    fun `TaskDto maps fields and nested initial state info when present`() {
        val dto = TaskDto(
            uid = 1L,
            taskTypeUid = 2L,
            parametersValues = mapOf(1L to "ON"),
            initialTaskStateInfo = stateInfoDto,
        )

        val result = dto.toModel()

        assertEquals(1L, result.uid)
        assertEquals(2L, result.taskTypeUid)
        assertEquals(mapOf(1L to "ON"), result.parametersValues)
        assertEquals("DONE", result.initialTaskStateInfo?.state)
    }

    @Test
    fun `TaskDto maps null initial state info`() {
        val dto = TaskDto(uid = 1L, taskTypeUid = 2L, initialTaskStateInfo = null)

        val result = dto.toModel()

        assertNull(result.initialTaskStateInfo)
    }

    @Test
    fun `TaskStateInfoHistoryEntryDto maps fields and builds task key from device uuid`() {
        val dto = TaskStateInfoHistoryEntryDto(
            stateInfoId = 99L,
            taskUid = 5L,
            taskTypeUid = 6L,
            dateTime = stateInfoDto.dateTime,
            state = "DONE",
            progress = 1.0,
            errorMessage = "",
        )

        val result = dto.toModel(deviceUuid = "device-1")

        assertEquals(99L, result.stateInfoId)
        assertEquals("device-1", result.taskKey.deviceUuid)
        assertEquals(5L, result.taskKey.taskUid)
        assertEquals(6L, result.taskKey.taskTypeUid)
        assertEquals("DONE", result.stateInfo.state)
        assertEquals(1.0, result.stateInfo.progress)
    }
}
