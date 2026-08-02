package com.server.croniot.services

import com.server.croniot.application.ApplicationScope
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.TaskRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import com.server.croniot.mqtt.MqttController
import croniot.messages.MessageAddTask
import croniot.models.Device
import croniot.models.ParameterTask
import croniot.models.Task
import croniot.models.TaskStateInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TaskServiceTest {

    private val taskRepository: TaskRepository = mockk(relaxUnitFun = true)
    private val taskTypeRepository: TaskTypeRepository = mockk()
    private val deviceRepository: DeviceRepository = mockk()
    private val applicationScope = ApplicationScope()

    private val service = TaskService(
        taskRepository = taskRepository,
        taskTypeRepository = taskTypeRepository,
        deviceRepository = deviceRepository,
        applicationScope = applicationScope,
    )

    private val device = Device(uuid = "device-uuid", name = "d", iot = true)

    private val message = MessageAddTask(
        deviceUuid = "device-uuid",
        taskTypeUid = "42",
        parametersValues = mapOf(1L to "on"),
    )

    @BeforeEach
    fun setUp() {
        mockkObject(MqttController)
        coEvery { MqttController.sendNewTask(any(), any()) } returns Unit
        coEvery { MqttController.sendTaskToDevice(any(), any()) } returns Unit
        coEvery { MqttController.requestTaskStateInfoSync(any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(MqttController)
        applicationScope.shutdown()
    }

    @Test
    fun `WHEN device is unknown THEN addTask returns failure result and skips creation`() {
        every { deviceRepository.getLazy("device-uuid") } returns null

        val result = service.addTask(message)

        assertFalse(result.success)
        verify(exactly = 0) { taskRepository.create(any<Task>()) }
    }

    @Test
    fun `WHEN deviceId cannot be resolved THEN addTask returns failure result`() {
        every { deviceRepository.getLazy("device-uuid") } returns device
        every { deviceRepository.getId("device-uuid") } returns null

        val result = service.addTask(message)

        assertFalse(result.success)
        verify(exactly = 0) { taskRepository.create(any<Task>()) }
    }

    @Test
    fun `WHEN task type does not exist for device THEN addTask returns failure result`() {
        every { deviceRepository.getLazy("device-uuid") } returns device
        every { deviceRepository.getId("device-uuid") } returns 5L
        every { taskTypeRepository.exists(taskTypeUid = 42L, deviceId = 5L) } returns false

        val result = service.addTask(message)

        assertFalse(result.success)
        verify(exactly = 0) { taskRepository.create(any<Task>()) }
    }

    @Test
    fun `WHEN parameters are resolved THEN addTask persists the task and returns success`() {
        val parameterTask = ParameterTask(
            uid = 1L,
            name = "power",
            type = "bool",
            unit = "",
            description = "",
        )
        every { deviceRepository.getLazy("device-uuid") } returns device
        every { deviceRepository.getId("device-uuid") } returns 5L
        every { taskTypeRepository.exists(taskTypeUid = 42L, deviceId = 5L) } returns true
        every { taskTypeRepository.getId(deviceId = 5L, taskTypeUid = 42L) } returns 7L
        every { taskTypeRepository.getParameterTaskByUid(parameterUid = 1L, taskTypeId = 7L) } returns parameterTask
        val taskSlot = slot<Task>()
        every { taskRepository.create(capture(taskSlot)) } returns Unit

        val result = service.addTask(message)

        assertTrue(result.success)
        assertEquals(42L, taskSlot.captured.taskTypeUid)
        assertEquals(mapOf(parameterTask to "on"), taskSlot.captured.parametersValues)
        verify(exactly = 1) { taskRepository.create(any<Task>()) }
        verify(exactly = 1) { taskRepository.createState(any(), any()) }
    }

    @Test
    fun `WHEN parameters cannot be resolved THEN addTask skips them without failing`() {
        every { deviceRepository.getLazy("device-uuid") } returns device
        every { deviceRepository.getId("device-uuid") } returns 5L
        every { taskTypeRepository.exists(taskTypeUid = 42L, deviceId = 5L) } returns true
        every { taskTypeRepository.getId(deviceId = 5L, taskTypeUid = 42L) } returns 7L
        every { taskTypeRepository.getParameterTaskByUid(parameterUid = 1L, taskTypeId = 7L) } returns null
        val taskSlot = slot<Task>()
        every { taskRepository.create(capture(taskSlot)) } returns Unit

        val result = service.addTask(message)

        assertTrue(result.success)
        assertTrue(taskSlot.captured.parametersValues.isEmpty())
    }

    @Test
    fun `WHEN repository throws THEN addTask returns failure result`() {
        every { deviceRepository.getLazy("device-uuid") } throws RuntimeException("boom")

        val result = service.addTask(message)

        assertFalse(result.success)
    }

    @Test
    fun `WHEN getTasksByDeviceUuid is called THEN it maps repository tasks to dtos`() {
        val parameterTask = ParameterTask(uid = 1L, name = "power", type = "bool", unit = "", description = "")
        val stateInfo = TaskStateInfo(
            taskUid = 100L,
            dateTime = ZonedDateTime.parse("2026-04-19T10:00:00Z"),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )
        val task = Task(
            uid = 100L,
            parametersValues = mapOf(parameterTask to "on"),
            taskTypeUid = 42L,
            mostRecentStateInfo = stateInfo,
        )
        every { taskRepository.getAll("device-uuid") } returns listOf(task)

        val dtos = service.getTasksByDeviceUuid("device-uuid")

        assertEquals(1, dtos.size)
        assertEquals(100L, dtos.first().uid)
        assertEquals(42L, dtos.first().taskTypeUid)
        assertEquals(mapOf(1L to "on"), dtos.first().parametersValues)
        assertEquals("RUNNING", dtos.first().initialTaskStateInfo?.state)
    }

    @Test
    fun `WHEN requestTaskStateInfoSync is called THEN it always returns a success result`() {
        val result = service.requestTaskStateInfoSync("device-uuid", 42L)

        assertTrue(result.success)
    }
}
