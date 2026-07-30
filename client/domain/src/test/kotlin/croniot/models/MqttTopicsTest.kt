package croniot.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class MqttTopicsTest {

    @Test
    fun `newTasks builds the per-device topic`() {
        assertThat(MqttTopics.newTasks("device-1")).isEqualTo("/device-1/newTasks")
    }

    @Test
    fun `sensorData builds the server-to-app topic`() {
        assertThat(MqttTopics.sensorData("device-1")).isEqualTo("/server_to_app/device-1/sensor_data")
    }

    @Test
    fun `taskProgressWildcard uses a plus wildcard for task type and task uid`() {
        assertThat(MqttTopics.taskProgressWildcard("device-1"))
            .isEqualTo("/server_to_devices/device-1/task_types/+/tasks/+/progress")
    }

    @Test
    fun `taskProgress fills in the concrete task type and task uid`() {
        assertThat(MqttTopics.taskProgress("device-1", taskTypeUid = 5L, taskUid = 9L))
            .isEqualTo("/server_to_devices/device-1/task_types/5/tasks/9/progress")
    }

    @Test
    fun `featureFlagUpdate builds the per-flag topic`() {
        assertThat(MqttTopics.featureFlagUpdate("dark_mode")).isEqualTo("/server/feature_flags/dark_mode")
    }

    @Test
    fun `FEATURE_FLAG_UPDATES_WILDCARD is a fixed wildcard topic`() {
        assertThat(MqttTopics.FEATURE_FLAG_UPDATES_WILDCARD).isEqualTo("/server/feature_flags/+")
    }
}
