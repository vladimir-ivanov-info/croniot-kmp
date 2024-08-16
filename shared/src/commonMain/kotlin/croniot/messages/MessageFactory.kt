package croniot.messages

import com.google.gson.Gson

object MessageFactory {

    fun parseMessageLogin(message: String) : MessageLogin{
        return Gson().fromJson(message, MessageLogin::class.java)
    }

   /* inline fun <reified T> Gson.fromJson(json: String): T =
        this.fromJson(json, T::class.java)
*/

    inline fun <reified T> fromJson(message: String): T {
        return Gson().fromJson(message, T::class.java)
    }

}