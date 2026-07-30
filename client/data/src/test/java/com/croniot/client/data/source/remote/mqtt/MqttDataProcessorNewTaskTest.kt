package com.croniot.client.data.source.remote.mqtt

import android.util.Log
import com.croniot.client.domain.models.Task
import croniot.messages.MessageFactory
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MqttDataProcessorNewTaskTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `process maps task dto and overrides deviceUuid with the configured one`() {
        val receivedTasks = mutableListOf<Task>()
        val processor = MqttDataProcessorNewTask(deviceUuid = "device-1", onNewTask = { receivedTasks.add(it) })
        val taskDto = croniot.models.dto.TaskDto(uid = 1L, taskTypeUid = 10L, parametersValues = mapOf(1L to "on"))
        val json = MessageFactory.toJson(taskDto)

        processor.process("some/topic", json)

        assertEquals(1, receivedTasks.size)
        assertEquals("device-1", receivedTasks.first().deviceUuid)
        assertEquals(1L, receivedTasks.first().uid)
        assertEquals(10L, receivedTasks.first().taskTypeUid)
    }

    @Test
    fun `process with invalid json does not invoke onNewTask`() {
        val receivedTasks = mutableListOf<Task>()
        val processor = MqttDataProcessorNewTask(deviceUuid = "device-1", onNewTask = { receivedTasks.add(it) })

        processor.process("some/topic", "not valid json {")

        assertEquals(0, receivedTasks.size)
    }

    @Test
    fun `process with non-string data does not invoke onNewTask`() {
        val receivedTasks = mutableListOf<Task>()
        val processor = MqttDataProcessorNewTask(deviceUuid = "device-1", onNewTask = { receivedTasks.add(it) })

        processor.process("some/topic", 12345)

        assertEquals(0, receivedTasks.size)
    }
}
