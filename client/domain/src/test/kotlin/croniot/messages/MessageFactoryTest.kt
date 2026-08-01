package croniot.messages

import assertk.assertThat
import assertk.assertions.isEqualTo
import croniot.models.DeviceToken
import org.junit.jupiter.api.Test

class MessageFactoryTest {

    @Test
    fun `WHEN a simple data class is serialized and deserialized THEN it round-trips correctly`() {
        val token = DeviceToken(deviceId = 42L, token = "abc-123")

        val json = MessageFactory.toJson(token)
        val result = MessageFactory.fromJson<DeviceToken>(json)

        assertThat(result).isEqualTo(token)
    }

    @Test
    fun `WHEN a class has nullable and map fields THEN toJson and fromJson round-trip it correctly`() {
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
    fun `WHEN json contains unknown keys THEN fromJson ignores them instead of failing`() {
        val json = """{"deviceId":1,"token":"t","unexpectedField":"whatever"}"""

        val result = MessageFactory.fromJson<DeviceToken>(json)

        assertThat(result).isEqualTo(DeviceToken(deviceId = 1L, token = "t"))
    }

    @Test
    fun `WHEN a plain data class is used THEN fromJsonWithZonedDateTime round-trips like fromJson`() {
        val token = DeviceToken(deviceId = 7L, token = "xyz")

        val json = MessageFactory.toJson(token)
        val result = MessageFactory.fromJsonWithZonedDateTime<DeviceToken>(json)

        assertThat(result).isEqualTo(token)
    }
}
