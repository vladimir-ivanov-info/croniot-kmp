package croniot.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class SensorDataTaskModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun device() = Device(uuid = "device-1", name = "My Device", iot = true)

    @Test
    fun `SensorData serializes and deserializes`() {
        val original = SensorData(
            device = device(),
            sensorType = SensorType(uid = 1L, name = "temp", description = "", parameters = emptyList()),
            value = "23.5",
            dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
        )

        val decoded = json.decodeFromString<SensorData>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `Task defaults mostRecentStateInfo to null`() {
        val parameter = ParameterTask(uid = 1L, name = "duration", type = "int", unit = "s", description = "")
        val task = Task(uid = 1L, parametersValues = mapOf(parameter to "10"), taskTypeUid = 10L)

        assertThat(task.mostRecentStateInfo).isEqualTo(null)
        assertThat(task.parametersValues[parameter]).isEqualTo("10")
    }

    @Test
    fun `Task carries an explicit mostRecentStateInfo and is equal by value`() {
        val stateInfo = TaskStateInfo(
            taskUid = 1L,
            dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        )
        val task = Task(uid = 1L, parametersValues = emptyMap(), taskTypeUid = 10L, mostRecentStateInfo = stateInfo)
        val copy = task.copy()

        assertThat(copy).isEqualTo(task)
        assertThat(copy.mostRecentStateInfo).isEqualTo(stateInfo)
    }

    @Test
    fun `TaskStateInfo serializes and deserializes`() {
        val original = TaskStateInfo(
            taskUid = 5L,
            dateTime = ZonedDateTime.of(2024, 6, 15, 12, 30, 0, 0, ZoneOffset.UTC),
            state = "ERROR",
            progress = 0.0,
            errorMessage = "boom",
        )

        val decoded = json.decodeFromString<TaskStateInfo>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }
}
