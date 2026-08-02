package croniot.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import croniot.models.Account
import croniot.models.Device
import croniot.models.SensorType
import croniot.models.TaskType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class MessageSerializationRoundtripTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `WHEN deviceToken is null THEN LoginDto roundtrips correctly`() {
        val original = LoginDto(
            email = "user@example.com",
            password = "secret",
            deviceUuid = "device-1",
            deviceToken = null,
            deviceProperties = mapOf("model" to "Pixel"),
        )

        val decoded = json.decodeFromString<LoginDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN deviceToken is non-null THEN LoginDto roundtrips correctly`() {
        val original = LoginDto(
            email = "user@example.com",
            password = "secret",
            deviceUuid = "device-1",
            deviceToken = "token-abc",
            deviceProperties = emptyMap(),
        )

        val decoded = json.decodeFromString<LoginDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN MessageRegisterAccount roundtrips correctly`() {
        val original = MessageRegisterAccount(
            accountUuid = "acc-1",
            nickname = "nick",
            email = "user@example.com",
            password = "pass",
        )

        val decoded = json.decodeFromString<MessageRegisterAccount>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN MessageRegisterDevice roundtrips correctly`() {
        val original = MessageRegisterDevice(
            accountEmail = "user@example.com",
            accountPassword = "pass",
            deviceUuid = "device-1",
            deviceName = "My Device",
            deviceDescription = "A test device",
        )

        val decoded = json.decodeFromString<MessageRegisterDevice>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains a nested SensorType THEN MessageRegisterSensorType roundtrips correctly`() {
        val sensorType = SensorType(uid = 1L, name = "Temperature", description = "desc", parameters = emptyList())
        val original = MessageRegisterSensorType(deviceUuid = "device-1", deviceToken = "token", sensorType = sensorType)

        val decoded = json.decodeFromString<MessageRegisterSensorType>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains a nested TaskType THEN MessageRegisterTaskType roundtrips correctly`() {
        val taskType = TaskType(uid = 1L, name = "Water", description = "desc", parameters = emptyList())
        val original = MessageRegisterTaskType(deviceUuid = "device-1", deviceToken = "token", taskType = taskType)

        val decoded = json.decodeFromString<MessageRegisterTaskType>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it has multiple parameter values THEN MessageTask roundtrips correctly`() {
        val original = MessageTask(taskTypeUid = 10L, parametersValues = mapOf(1L to "on", 2L to "50"), taskUid = 99L)

        val decoded = json.decodeFromString<MessageTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN parameter values are empty THEN MessageTask roundtrips correctly`() {
        val original = MessageTask(taskTypeUid = 10L, parametersValues = emptyMap(), taskUid = 0L)

        val decoded = json.decodeFromString<MessageTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN MessageSensorData roundtrips correctly`() {
        val original = MessageSensorData(sensorTypeId = 5L, value = "23.5")

        val decoded = json.decodeFromString<MessageSensorData>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains a nested Account with devices THEN MessageAccountInfo roundtrips correctly`() {
        val account = Account(
            uuid = "acc-1",
            nickname = "nick",
            email = "user@example.com",
            devices = listOf(Device(uuid = "device-1", name = "Device", description = "desc", iot = true)),
        )
        val original = MessageAccountInfo(account = account)

        val decoded = json.decodeFromString<MessageAccountInfo>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN MessageGetAccountInfo roundtrips correctly`() {
        val original = MessageGetAccountInfo(token = "jwt-token")

        val decoded = json.decodeFromString<MessageGetAccountInfo>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN MessageRequestTaskStateInfoSync roundtrips correctly`() {
        val original = MessageRequestTaskStateInfoSync(deviceUuid = "device-1", taskTypeUid = "10")

        val decoded = json.decodeFromString<MessageRequestTaskStateInfoSync>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it has multiple parameter values THEN MessageAddTask roundtrips correctly`() {
        val original = MessageAddTask(deviceUuid = "device-1", taskTypeUid = "10", parametersValues = mapOf(1L to "on"))

        val decoded = json.decodeFromString<MessageAddTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }
}
