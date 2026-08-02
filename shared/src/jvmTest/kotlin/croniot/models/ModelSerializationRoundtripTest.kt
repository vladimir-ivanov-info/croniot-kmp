package croniot.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import croniot.models.dto.TaskStateInfoHistoryEntryDto
import croniot.models.errors.ErrorResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ModelSerializationRoundtripTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `WHEN it contains nested devices THEN Account roundtrips correctly`() {
        val original = Account(
            uuid = "acc-1",
            nickname = "nick",
            email = "user@example.com",
            devices = listOf(Device(uuid = "device-1", name = "Device 1", iot = true)),
        )

        val decoded = json.decodeFromString<Account>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it has no devices THEN Account roundtrips correctly`() {
        val original = Account(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())

        val decoded = json.decodeFromString<Account>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains nested sensor and task types THEN Device roundtrips correctly`() {
        val sensorType = SensorType(uid = 1L, name = "Temp", description = "desc", parameters = emptyList())
        val taskType = TaskType(uid = 2L, name = "Water", description = "desc")
        val original = Device(
            uuid = "device-1",
            name = "Device",
            description = "desc",
            iot = true,
            sensorTypes = listOf(sensorType),
            taskTypes = listOf(taskType),
        )

        val decoded = json.decodeFromString<Device>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN description and type lists use their defaults THEN Device roundtrips correctly`() {
        val original = Device(uuid = "device-1", name = "Device", iot = false)

        val decoded = json.decodeFromString<Device>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.description).isEqualTo("")
    }

    @Test
    fun `WHEN it contains a nested ParameterSensor list THEN SensorType roundtrips correctly`() {
        val parameter = ParameterSensor(
            uid = 1L,
            name = "threshold",
            type = "number",
            unit = "celsius",
            description = "desc",
            constraints = mapOf("min" to "0", "max" to "100"),
        )
        val original = SensorType(uid = 1L, name = "Temperature", description = "desc", parameters = listOf(parameter))

        val decoded = json.decodeFromString<SensorType>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN parameters use their default empty value THEN TaskType roundtrips correctly`() {
        val original = TaskType(uid = 1L, name = "Water", description = "desc")

        val decoded = json.decodeFromString<TaskType>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.parameters).isEqualTo(emptyList())
    }

    @Test
    fun `WHEN constraints use their default empty value THEN ParameterTask roundtrips correctly`() {
        val original = ParameterTask(uid = 1L, name = "duration", type = "number", unit = "s", description = "desc")

        val decoded = json.decodeFromString<ParameterTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.constraints).isEqualTo(emptyMap())
    }

    @Test
    fun `WHEN constraints are non-empty THEN ParameterSensor roundtrips correctly`() {
        val original = ParameterSensor(
            uid = 1L,
            name = "threshold",
            type = "number",
            unit = "c",
            description = "desc",
            constraints = mapOf("min" to "-10", "max" to "50"),
        )

        val decoded = json.decodeFromString<ParameterSensor>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN DeviceToken roundtrips correctly`() {
        val original = DeviceToken(deviceId = 42L, token = "device-token-abc")

        val decoded = json.decodeFromString<DeviceToken>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN TaskProgressUpdate roundtrips correctly`() {
        val original = TaskProgressUpdate(
            taskTypeUid = 10L,
            taskUid = 1L,
            state = "RUNNING",
            progress = 42.5,
            errorMessage = "",
        )

        val decoded = json.decodeFromString<TaskProgressUpdate>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN details use their default empty value THEN ErrorResponse roundtrips correctly`() {
        val original = ErrorResponse(code = "NOT_FOUND", message = "Resource not found")

        val decoded = json.decodeFromString<ErrorResponse>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.details).isEqualTo(emptyMap())
    }

    @Test
    fun `WHEN details are non-empty THEN ErrorResponse roundtrips correctly`() {
        val original = ErrorResponse(
            code = "VALIDATION",
            message = "Invalid field",
            details = mapOf("field" to "email"),
        )

        val decoded = json.decodeFromString<ErrorResponse>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains a ZonedDateTime THEN TaskStateInfoHistoryEntryDto roundtrips correctly`() {
        val original = TaskStateInfoHistoryEntryDto(
            stateInfoId = 1L,
            taskUid = 5L,
            taskTypeUid = 10L,
            dateTime = ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC),
            state = "COMPLETED",
            progress = 100.0,
            errorMessage = "",
        )

        val decoded = json.decodeFromString<TaskStateInfoHistoryEntryDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN stateInfoId is not provided THEN TaskStateInfoHistoryEntryDto defaults it to minus one`() {
        val original = TaskStateInfoHistoryEntryDto(
            taskUid = 5L,
            taskTypeUid = 10L,
            dateTime = ZonedDateTime.now(),
            state = "RUNNING",
            progress = 0.0,
            errorMessage = "",
        )

        assertThat(original.stateInfoId).isEqualTo(-1L)
    }
}
