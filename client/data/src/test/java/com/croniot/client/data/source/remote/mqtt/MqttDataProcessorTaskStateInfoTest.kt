package com.croniot.client.data.source.remote.mqtt

import croniot.models.TaskKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

/**
 * Tests for the MQTT topic parsing logic in MqttDataProcessorTaskProgress.
 * `parseProgressTopic` is private, accessed via reflection since it contains
 * pure parsing logic with no Android dependencies.
 */
class MqttDataProcessorTaskStateInfoTest {

    private lateinit var processor: MqttDataProcessorTaskStateInfo
    private lateinit var parseMethod: Method

    @BeforeEach
    fun setUp() {
        processor = MqttDataProcessorTaskStateInfo(onNewData = {})
        parseMethod = MqttDataProcessorTaskStateInfo::class.java
            .getDeclaredMethod("parseProgressTopic", String::class.java)
            .also { it.isAccessible = true }
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
}
