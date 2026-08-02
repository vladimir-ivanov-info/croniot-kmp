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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import java.time.ZonedDateTime

class MqttDataProcessorTaskStateInfoTest {

    private lateinit var processor: MqttDataProcessorTaskStateInfo
    private lateinit var parseMethod: Method

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        processor = MqttDataProcessorTaskStateInfo(onNewData = {})
        parseMethod = MqttDataProcessorTaskStateInfo::class.java
            .getDeclaredMethod("parseTaskStateInfoTopic", String::class.java)
            .also { it.isAccessible = true }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun parseProgressTopic(topic: String): TaskKey? =
        parseMethod.invoke(processor, topic) as? TaskKey

    @Test
    fun `WHEN topic is valid THEN it returns the correct TaskKey`() {
        val topic = "server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress"
        val key = parseProgressTopic(topic)

        assertEquals(TaskKey(deviceUuid = "dev-uuid-1", taskTypeUid = 10L, taskUid = 99L), key)
    }

    @Test
    fun `WHEN a valid topic has a leading slash THEN it is parsed correctly`() {
        val topic = "/server_to_devices/dev-uuid/task_types/1/tasks/2/progress"
        val key = parseProgressTopic(topic)

        assertEquals(TaskKey(deviceUuid = "dev-uuid", taskTypeUid = 1L, taskUid = 2L), key)
    }

    @Test
    fun `WHEN topic has too few segments THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/99"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN topic has too many segments THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/99/progress/extra"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN the first segment is wrong THEN it returns null`() {
        val topic = "wrong_prefix/dev-uuid/task_types/10/tasks/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN the third segment is wrong THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/wrong_segment/10/tasks/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN the fifth segment is wrong THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/wrong_segment/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN the last segment is wrong THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/99/wrong_suffix"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN taskTypeUid is non-numeric THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/abc/tasks/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN taskUid is non-numeric THEN it returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/xyz/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `WHEN topic is empty THEN it returns null`() {
        assertNull(parseProgressTopic(""))
    }

    @Test
    fun `WHEN topic is valid THEN process invokes onNewData with the parsed event`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })
        val dto = TaskStateInfoDto(
            dateTime = ZonedDateTime.now(),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )
        val json = MessageFactory.toJson(dto)
        val topic = "server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress"

        eventProcessor.process(topic, json)

        assertEquals(1, receivedEvents.size)
        assertEquals(TaskKey(deviceUuid = "dev-uuid-1", taskTypeUid = 10L, taskUid = 99L), receivedEvents.first().key)
        assertEquals("RUNNING", receivedEvents.first().info.state)
    }

    @Test
    fun `WHEN topic does not match THEN process does not invoke onNewData`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })
        val dto = TaskStateInfoDto(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.0, errorMessage = "")

        eventProcessor.process("wrong/topic", MessageFactory.toJson(dto))

        assertTrue(receivedEvents.isEmpty())
    }

    @Test
    fun `WHEN json is malformed THEN process swallows the exception`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })
        val topic = "server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress"

        eventProcessor.process(topic, "not valid json {")

        assertTrue(receivedEvents.isEmpty())
    }

    @Test
    fun `WHEN data is non-string THEN process swallows the exception`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })

        eventProcessor.process("server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress", 12345)

        assertTrue(receivedEvents.isEmpty())
    }
}
