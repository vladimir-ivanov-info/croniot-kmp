package croniot.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import croniot.models.DeviceToken
import org.junit.jupiter.api.Test

class MessageFactoryTest {

    @Test
    fun `toJson followed by fromJson round-trips a simple data class`() {
        val token = DeviceToken(deviceId = 42L, token = "abc-123")

        val json = MessageFactory.toJson(token)
        val result = MessageFactory.fromJson<DeviceToken>(json)

        assertThat(result).isEqualTo(token)
    }

    @Test
    fun `toJson followed by fromJson round-trips a class with nullable and map fields`() {
        val dto = LoginDto(
            email = "user@example.com",
            password = "secret",
            deviceUuid = "device-1",
            deviceToken = null,
            deviceProperties = mapOf("os" to "android"),
        )

        val json = MessageFactory.toJson(dto)
        val result = MessageFactory.fromJson<LoginDto>(json)

        assertThat(result).isEqualTo(dto)
    }

    @Test
    fun `fromJson ignores unknown keys instead of failing`() {
        val json = """{"deviceId":1,"token":"t","unexpectedField":"whatever"}"""

        val result = MessageFactory.fromJson<DeviceToken>(json)

        assertThat(result).isEqualTo(DeviceToken(deviceId = 1L, token = "t"))
    }

    @Test
    fun `fromJsonWithZonedDateTime round-trips like fromJson for plain data classes`() {
        val token = DeviceToken(deviceId = 7L, token = "xyz")

        val json = MessageFactory.toJson(token)
        val result = MessageFactory.fromJsonWithZonedDateTime<DeviceToken>(json)

        assertThat(result).isEqualTo(token)
    }
}
