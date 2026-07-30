package croniot.serialization

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ZonedDateTimeSerializerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Wrapper(
        @Serializable(with = ZonedDateTimeSerializer::class)
        val dateTime: ZonedDateTime,
    )

    @Test
    fun `serialize then deserialize roundtrips to an equal ZonedDateTime`() {
        val original = ZonedDateTime.of(2024, 3, 15, 10, 30, 0, 0, ZoneOffset.UTC)

        val encoded = json.encodeToString(Wrapper(original))
        val decoded = json.decodeFromString<Wrapper>(encoded)

        assertThat(decoded.dateTime).isEqualTo(original)
    }

    @Test
    fun `serialize produces an ISO-8601 zoned date time string`() {
        val dateTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        val encoded = json.encodeToString(Wrapper(dateTime))

        assertThat(encoded).isEqualTo("""{"dateTime":"2024-01-01T00:00:00Z"}""")
    }

    @Test
    fun `deserialize parses an ISO-8601 string with a non-UTC offset`() {
        val encoded = """{"dateTime":"2024-06-01T12:00:00+02:00"}"""

        val decoded = json.decodeFromString<Wrapper>(encoded)

        assertThat(decoded.dateTime.offset).isEqualTo(ZoneOffset.ofHours(2))
        assertThat(decoded.dateTime.hour).isEqualTo(12)
    }

    @Test
    fun `descriptor identifies the serializer as a ZonedDateTime string primitive`() {
        assertThat(ZonedDateTimeSerializer.descriptor.serialName).isEqualTo("ZonedDateTime")
    }

    @Test
    fun `roundtrip preserves nanosecond precision`() {
        val original = ZonedDateTime.of(2024, 5, 20, 8, 15, 30, 123_000_000, ZoneOffset.UTC)

        val encoded = json.encodeToString(Wrapper(original))
        val decoded = json.decodeFromString<Wrapper>(encoded)

        assertThat(decoded.dateTime).isEqualTo(original)
    }
}
