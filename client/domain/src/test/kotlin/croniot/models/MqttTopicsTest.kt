package croniot.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class MqttTopicsTest {

    @Test
    fun `WHEN newTasks is called with a device id THEN it builds the per-device topic`() {
        assertThat(MqttTopics.newTasks("device-1")).isEqualTo("/device-1/newTasks")
    }

    @Test
    fun `WHEN sensorData is called with a device id THEN it builds the server-to-app topic`() {
        assertThat(MqttTopics.sensorData("device-1")).isEqualTo("/server_to_app/device-1/sensor_data")
    }

    @Test
    fun `WHEN taskProgressWildcard is called THEN it uses a plus wildcard for task type and task uid`() {
        assertThat(MqttTopics.taskProgressWildcard("device-1"))
            .isEqualTo("/server_to_devices/device-1/task_types/+/tasks/+/progress")
    }

    @Test
    fun `WHEN taskProgress is called with a task type and task uid THEN it fills in the concrete values`() {
        assertThat(MqttTopics.taskProgress("device-1", taskTypeUid = 5L, taskUid = 9L))
            .isEqualTo("/server_to_devices/device-1/task_types/5/tasks/9/progress")
    }

    @Test
    fun `WHEN featureFlagUpdate is called with a flag name THEN it builds the per-flag topic`() {
        assertThat(MqttTopics.featureFlagUpdate("dark_mode")).isEqualTo("/server/feature_flags/dark_mode")
    }

    @Test
    fun `WHEN FEATURE_FLAG_UPDATES_WILDCARD is accessed THEN it is a fixed wildcard topic`() {
        assertThat(MqttTopics.FEATURE_FLAG_UPDATES_WILDCARD).isEqualTo("/server/feature_flags/+")
    }
}
