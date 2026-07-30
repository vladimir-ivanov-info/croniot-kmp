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
    fun `LoginDto roundtrips with a null deviceToken`() {
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
    fun `LoginDto roundtrips with a non-null deviceToken`() {
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
    fun `MessageRegisterAccount roundtrips`() {
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
    fun `MessageRegisterDevice roundtrips`() {
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
    fun `MessageRegisterSensorType roundtrips with nested SensorType`() {
        val sensorType = SensorType(uid = 1L, name = "Temperature", description = "desc", parameters = emptyList())
        val original = MessageRegisterSensorType(deviceUuid = "device-1", deviceToken = "token", sensorType = sensorType)

        val decoded = json.decodeFromString<MessageRegisterSensorType>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageRegisterTaskType roundtrips with nested TaskType`() {
        val taskType = TaskType(uid = 1L, name = "Water", description = "desc", parameters = emptyList())
        val original = MessageRegisterTaskType(deviceUuid = "device-1", deviceToken = "token", taskType = taskType)

        val decoded = json.decodeFromString<MessageRegisterTaskType>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageTask roundtrips with multiple parameter values`() {
        val original = MessageTask(taskTypeUid = 10L, parametersValues = mapOf(1L to "on", 2L to "50"), taskUid = 99L)

        val decoded = json.decodeFromString<MessageTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageTask roundtrips with empty parameter values`() {
        val original = MessageTask(taskTypeUid = 10L, parametersValues = emptyMap(), taskUid = 0L)

        val decoded = json.decodeFromString<MessageTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageSensorData roundtrips`() {
        val original = MessageSensorData(sensorTypeId = 5L, value = "23.5")

        val decoded = json.decodeFromString<MessageSensorData>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageAccountInfo roundtrips with nested Account and devices`() {
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
    fun `MessageGetAccountInfo roundtrips`() {
        val original = MessageGetAccountInfo(token = "jwt-token")

        val decoded = json.decodeFromString<MessageGetAccountInfo>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageRequestTaskStateInfoSync roundtrips`() {
        val original = MessageRequestTaskStateInfoSync(deviceUuid = "device-1", taskTypeUid = "10")

        val decoded = json.decodeFromString<MessageRequestTaskStateInfoSync>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `MessageAddTask roundtrips with multiple parameter values`() {
        val original = MessageAddTask(deviceUuid = "device-1", taskTypeUid = "10", parametersValues = mapOf(1L to "on"))

        val decoded = json.decodeFromString<MessageAddTask>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }
}
