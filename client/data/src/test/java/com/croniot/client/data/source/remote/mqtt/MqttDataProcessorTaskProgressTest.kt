package com.croniot.client.data.source.remote.mqtt

import android.util.Log
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import croniot.messages.MessageFactory
import croniot.models.TaskKey
import croniot.models.dto.TaskStateInfoDto
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class MqttDataProcessorTaskProgressTest {

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

    private fun stateInfoDto() = TaskStateInfoDto(
        dateTime = ZonedDateTime.now(),
        state = "RUNNING",
        progress = 0.5,
        errorMessage = "",
    )

    @Test
    fun `process parses valid progress topic and emits TaskStateInfoEvent`() {
        val received = mutableListOf<TaskStateInfoEvent>()
        val processor = MqttDataProcessorTaskProgress(onNewData = { received.add(it) })
        val json = MessageFactory.toJson(stateInfoDto())

        processor.process("server_to_devices/device-1/task_types/10/tasks/99/progress", json)

        assertEquals(1, received.size)
        assertEquals(TaskKey("device-1", 10L, 99L), received.first().key)
        assertEquals("RUNNING", received.first().info.state)
    }

    @Test
    fun `process with malformed topic does not invoke callback`() {
        val received = mutableListOf<TaskStateInfoEvent>()
        val processor = MqttDataProcessorTaskProgress(onNewData = { received.add(it) })
        val json = MessageFactory.toJson(stateInfoDto())

        processor.process("wrong/topic/format", json)

        assertEquals(0, received.size)
    }

    @Test
    fun `process with invalid json does not invoke callback`() {
        val received = mutableListOf<TaskStateInfoEvent>()
        val processor = MqttDataProcessorTaskProgress(onNewData = { received.add(it) })

        processor.process("server_to_devices/device-1/task_types/10/tasks/99/progress", "not valid json {")

        assertEquals(0, received.size)
    }

    @Test
    fun `process with non-numeric taskTypeUid does not invoke callback`() {
        val received = mutableListOf<TaskStateInfoEvent>()
        val processor = MqttDataProcessorTaskProgress(onNewData = { received.add(it) })
        val json = MessageFactory.toJson(stateInfoDto())

        processor.process("server_to_devices/device-1/task_types/abc/tasks/99/progress", json)

        assertEquals(0, received.size)
    }
}
