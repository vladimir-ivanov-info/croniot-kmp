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
    fun `SensorTypeDto roundtrips with nested parameters`() {
        val original = SensorTypeDto(uid = 1L, name = "Temp", description = "desc", parameters = listOf(parameterSensorDto()))

        val decoded = json.decodeFromString<SensorTypeDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `TaskTypeDto roundtrips using its default uid`() {
        val original = TaskTypeDto(name = "Water", description = "desc", parameters = listOf(parameterTaskDto()))

        val decoded = json.decodeFromString<TaskTypeDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.uid).isEqualTo(0L)
    }

    @Test
    fun `ParameterSensorDto roundtrips`() {
        val original = parameterSensorDto()

        val decoded = json.decodeFromString<ParameterSensorDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `ParameterTaskDto roundtrips with empty constraints`() {
        val original = parameterTaskDto()

        val decoded = json.decodeFromString<ParameterTaskDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `DeviceDto roundtrips with nested sensor and task types`() {
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
    fun `AccountDto roundtrips with nested devices`() {
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
    fun `FeatureFlagDto roundtrips with a null description`() {
        val original = FeatureFlagDto(name = "dark_mode", enabled = true, description = null)

        val decoded = json.decodeFromString<FeatureFlagDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.description).isNull()
    }

    @Test
    fun `FeatureFlagDto roundtrips with a non-null description`() {
        val original = FeatureFlagDto(name = "beta", enabled = false, description = "Beta feature")

        val decoded = json.decodeFromString<FeatureFlagDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `LoginResultDto roundtrips with all optional fields present`() {
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
    fun `LoginResultDto roundtrips using default nulls for optional fields`() {
        val original = LoginResultDto(result = Result(success = false, message = "Invalid credentials"), accountDto = null, token = null)

        val decoded = json.decodeFromString<LoginResultDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.refreshToken).isNull()
        assertThat(decoded.accessTokenExpiresAtEpochSeconds).isNull()
    }

    @Test
    fun `RefreshTokenRequestDto roundtrips`() {
        val original = RefreshTokenRequestDto(refreshToken = "refresh-token-xyz")

        val decoded = json.decodeFromString<RefreshTokenRequestDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `RefreshTokenResultDto roundtrips with all fields present`() {
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
    fun `RefreshTokenResultDto roundtrips using default nulls`() {
        val original = RefreshTokenResultDto(result = Result(success = false, message = "expired"))

        val decoded = json.decodeFromString<RefreshTokenResultDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `LogoutRequestDto roundtrips`() {
        val original = LogoutRequestDto(refreshToken = "refresh-to-revoke")

        val decoded = json.decodeFromString<LogoutRequestDto>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `Result roundtrips with default empty message`() {
        val original = Result(success = true)

        val decoded = json.decodeFromString<Result>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
        assertThat(decoded.message).isEqualTo("")
    }

    @Test
    fun `Result roundtrips with a custom message`() {
        val original = Result(success = false, message = "Something went wrong")

        val decoded = json.decodeFromString<Result>(json.encodeToString(original))

        assertThat(decoded).isEqualTo(original)
    }
}
