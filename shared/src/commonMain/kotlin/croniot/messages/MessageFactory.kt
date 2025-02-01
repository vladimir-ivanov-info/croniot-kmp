package croniot.messages

import ZonedDateTimeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.time.ZonedDateTime

object MessageFactory {

    inline fun <reified T> fromJson(message: String): T {
        return Gson().fromJson(message, T::class.java)
    }

    inline fun <reified T> fromJsonWithZonedDateTime(message: String): T {
        val gsonZonedDateTime = GsonBuilder()
            .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
            .setPrettyPrinting()
            .create()
        return gsonZonedDateTime.fromJson(message, T::class.java)
    }
}
