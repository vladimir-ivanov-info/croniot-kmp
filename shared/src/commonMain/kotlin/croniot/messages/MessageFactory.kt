package croniot.messages

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MessageFactory {

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    inline fun <reified T> fromJson(message: String): T {
        return json.decodeFromString(message)
    }

    // En kotlinx.serialization, el mismo objeto Json con el serializador adecuado maneja ZonedDateTime
    inline fun <reified T> fromJsonWithZonedDateTime(message: String): T {
        return json.decodeFromString(message)
    }

    inline fun <reified T> toJson(value: T): String {
        return json.encodeToString(value)
    }
}
