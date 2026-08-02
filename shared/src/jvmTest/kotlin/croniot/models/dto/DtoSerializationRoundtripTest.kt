package croniot.models.dto

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import croniot.models.LoginResultDto
import croniot.models.LogoutRequestDto
import croniot.models.RefreshTokenRequestDto
import croniot.models.RefreshTokenResultDto
import croniot.models.Result
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class DtoSerializationRoundtripTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parameterSensorDto() = ParameterSensorDto(
        uid = 1L,
        name = "threshold",
        type = "number",
        unit = "c",
        description = "desc",
        constraints = mapOf("min" to "0"),
    )

    private fun parameterTaskDto() = ParameterTaskDto(
        uid = 2L,
        name = "duration",
        type = "number",
        unit = "s",
        description = "desc",
        constraints = emptyMap(),
    )

    @Test
    fun `WHEN it contains nested parameters THEN SensorTypeDto roundtrips correctly`() {
        val original = SensorTypeDto(uid = 1L, name = "Temp", description = "desc", parameters = listOf(parameterSensorDto()))

        val decoded = json.decodeFromString<SensorTypeDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN uid is not provided THEN TaskTypeDto roundtrips using its default uid`() {
        val original = TaskTypeDto(name = "Water", description = "desc", parameters = listOf(parameterTaskDto()))

        val decoded = json.decodeFromString<TaskTypeDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.uid).isEqualTo(0L)
    }

    @Test
    fun `WHEN encoded and decoded THEN ParameterSensorDto roundtrips correctly`() {
        val original = parameterSensorDto()

        val decoded = json.decodeFromString<ParameterSensorDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN constraints are empty THEN ParameterTaskDto roundtrips correctly`() {
        val original = parameterTaskDto()

        val decoded = json.decodeFromString<ParameterTaskDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains nested sensor and task types THEN DeviceDto roundtrips correctly`() {
        val original = DeviceDto(
            uuid = "device-1",
            name = "Device",
            description = "desc",
            iot = true,
            sensorTypes = listOf(SensorTypeDto(uid = 1L, name = "Temp", description = "desc", parameters = emptyList())),
            taskTypes = listOf(TaskTypeDto(uid = 2L, name = "Water", description = "desc", parameters = emptyList())),
        )

        val decoded = json.decodeFromString<DeviceDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN it contains nested devices THEN AccountDto roundtrips correctly`() {
        val device = DeviceDto(
            uuid = "device-1",
            name = "Device",
            description = "",
            iot = false,
            sensorTypes = emptyList(),
            taskTypes = emptyList(),
        )
        val original = AccountDto(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = listOf(device))

        val decoded = json.decodeFromString<AccountDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN description is null THEN FeatureFlagDto roundtrips correctly`() {
        val original = FeatureFlagDto(name = "dark_mode", enabled = true, description = null)

        val decoded = json.decodeFromString<FeatureFlagDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.description).isNull()
    }

    @Test
    fun `WHEN description is non-null THEN FeatureFlagDto roundtrips correctly`() {
        val original = FeatureFlagDto(name = "beta", enabled = false, description = "Beta feature")

        val decoded = json.decodeFromString<FeatureFlagDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN all optional fields are present THEN LoginResultDto roundtrips correctly`() {
        val account = AccountDto(uuid = "acc-1", nickname = "nick", email = "user@example.com", devices = emptyList())
        val original = LoginResultDto(
            result = Result(success = true),
            accountDto = account,
            token = "jwt",
            refreshToken = "refresh",
            accessTokenExpiresAtEpochSeconds = 1_800_000_000L,
        )

        val decoded = json.decodeFromString<LoginResultDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN optional fields use their default nulls THEN LoginResultDto roundtrips correctly`() {
        val original = LoginResultDto(result = Result(success = false, message = "Invalid credentials"), accountDto = null, token = null)

        val decoded = json.decodeFromString<LoginResultDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.refreshToken).isNull()
        assertThat(decoded.accessTokenExpiresAtEpochSeconds).isNull()
    }

    @Test
    fun `WHEN encoded and decoded THEN RefreshTokenRequestDto roundtrips correctly`() {
        val original = RefreshTokenRequestDto(refreshToken = "refresh-token-xyz")

        val decoded = json.decodeFromString<RefreshTokenRequestDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN all fields are present THEN RefreshTokenResultDto roundtrips correctly`() {
        val original = RefreshTokenResultDto(
            result = Result(success = true),
            token = "new-jwt",
            refreshToken = "new-refresh",
            accessTokenExpiresAtEpochSeconds = 1_900_000_000L,
        )

        val decoded = json.decodeFromString<RefreshTokenResultDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN fields use their default nulls THEN RefreshTokenResultDto roundtrips correctly`() {
        val original = RefreshTokenResultDto(result = Result(success = false, message = "expired"))

        val decoded = json.decodeFromString<RefreshTokenResultDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN encoded and decoded THEN LogoutRequestDto roundtrips correctly`() {
        val original = LogoutRequestDto(refreshToken = "refresh-to-revoke")

        val decoded = json.decodeFromString<LogoutRequestDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `WHEN message uses its default empty value THEN Result roundtrips correctly`() {
        val original = Result(success = true)

        val decoded = json.decodeFromString<Result>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.message).isEqualTo("")
    }

    @Test
    fun `WHEN a custom message is provided THEN Result roundtrips correctly`() {
        val original = Result(success = false, message = "Something went wrong")

        val decoded = json.decodeFromString<Result>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }
}
