package com.server.croniot.services

import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import croniot.messages.MessageRegisterTaskType
import croniot.models.Device
import croniot.models.TaskType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskTypeServiceTest {

    private val taskTypeRepository: TaskTypeRepository = mockk(relaxUnitFun = true)
    private val deviceRepository: DeviceRepository = mockk()
    private val deviceTokenRepository: DeviceTokenRepository = mockk()

    private val service = TaskTypeService(
        taskTypeRepository = taskTypeRepository,
        deviceRepository = deviceRepository,
        deviceTokenRepository = deviceTokenRepository,
    )

    private val taskType = TaskType(uid = 42L, name = "Toggle", description = "d")

    private val message = MessageRegisterTaskType(
        deviceUuid = "device-uuid",
        deviceToken = "device-token",
        taskType = taskType,
    )

    @Test
    fun `WHEN device id cannot be resolved THEN exists returns false`() {
        every { deviceRepository.getId("device-uuid") } returns null

        assertFalse(service.exists("device-uuid", 42L))
        verify(exactly = 0) { taskTypeRepository.exists(any(), any()) }
    }

    @Test
    fun `WHEN device id is resolved THEN exists delegates to repository`() {
        every { deviceRepository.getId("device-uuid") } returns 5L
        every { taskTypeRepository.exists(deviceId = 5L, taskTypeUid = 42L) } returns true

        assertTrue(service.exists("device-uuid", 42L))
    }

    @Test
    fun `WHEN device token does not resolve to a device THEN registerTaskType returns failure`() {
        every { deviceTokenRepository.getDevice("device-token") } returns null

        val result = service.registerTaskType(message)

        assertFalse(result.success)
        assertEquals("Incorrect device or token for task register process.", result.message)
        verify(exactly = 0) { taskTypeRepository.insert(any(), any()) }
    }

    @Test
    fun `WHEN resolved device uuid does not match message THEN registerTaskType returns failure`() {
        every { deviceTokenRepository.getDevice("device-token") } returns Device(
            uuid = "other-device",
            name = "d",
            iot = true,
        )

        val result = service.registerTaskType(message)

        assertFalse(result.success)
        assertEquals("Incorrect device or token for task register process.", result.message)
    }

    @Test
    fun `WHEN device id cannot be resolved THEN registerTaskType returns failure`() {
        every { deviceTokenRepository.getDevice("device-token") } returns Device(
            uuid = "device-uuid",
            name = "d",
            iot = true,
        )
        every { deviceRepository.getId("device-uuid") } returns null

        val result = service.registerTaskType(message)

        assertFalse(result.success)
        assertEquals("Incorrect device or token for task register process.", result.message)
        verify(exactly = 0) { taskTypeRepository.insert(any(), any()) }
    }

    @Test
    fun `WHEN inputs are valid THEN registerTaskType inserts the task type and returns success`() {
        every { deviceTokenRepository.getDevice("device-token") } returns Device(
            uuid = "device-uuid",
            name = "d",
            iot = true,
        )
        every { deviceRepository.getId("device-uuid") } returns 5L

        val result = service.registerTaskType(message)

        assertTrue(result.success)
        assertEquals("Task 42 registered", result.message)
        verify(exactly = 1) { taskTypeRepository.insert(taskType, 5L) }
    }
}
