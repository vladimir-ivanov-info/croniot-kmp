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
    fun `valid topic returns correct TaskKey`() {
        val topic = "server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress"
        val key = parseProgressTopic(topic)

        assertEquals(TaskKey(deviceUuid = "dev-uuid-1", taskTypeUid = 10L, taskUid = 99L), key)
    }

    @Test
    fun `valid topic with leading slash is parsed correctly`() {
        val topic = "/server_to_devices/dev-uuid/task_types/1/tasks/2/progress"
        val key = parseProgressTopic(topic)

        assertEquals(TaskKey(deviceUuid = "dev-uuid", taskTypeUid = 1L, taskUid = 2L), key)
    }

    @Test
    fun `too few segments returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/99"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `too many segments returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/99/progress/extra"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `wrong first segment returns null`() {
        val topic = "wrong_prefix/dev-uuid/task_types/10/tasks/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `wrong third segment returns null`() {
        val topic = "server_to_devices/dev-uuid/wrong_segment/10/tasks/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `wrong fifth segment returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/wrong_segment/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `wrong last segment returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/99/wrong_suffix"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `non-numeric taskTypeUid returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/abc/tasks/99/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `non-numeric taskUid returns null`() {
        val topic = "server_to_devices/dev-uuid/task_types/10/tasks/xyz/progress"
        assertNull(parseProgressTopic(topic))
    }

    @Test
    fun `empty topic returns null`() {
        assertNull(parseProgressTopic(""))
    }

    @Test
    fun `process invokes onNewData with the parsed event on a valid topic`() {
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
    fun `process does not invoke onNewData when the topic does not match`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })
        val dto = TaskStateInfoDto(dateTime = ZonedDateTime.now(), state = "RUNNING", progress = 0.0, errorMessage = "")

        eventProcessor.process("wrong/topic", MessageFactory.toJson(dto))

        assertTrue(receivedEvents.isEmpty())
    }

    @Test
    fun `process swallows exceptions from malformed json`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })
        val topic = "server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress"

        eventProcessor.process(topic, "not valid json {")

        assertTrue(receivedEvents.isEmpty())
    }

    @Test
    fun `process swallows exceptions from non-string data`() {
        val receivedEvents = mutableListOf<TaskStateInfoEvent>()
        val eventProcessor = MqttDataProcessorTaskStateInfo(onNewData = { receivedEvents.add(it) })

        eventProcessor.process("server_to_devices/dev-uuid-1/task_types/10/tasks/99/progress", 12345)

        assertTrue(receivedEvents.isEmpty())
    }
}
