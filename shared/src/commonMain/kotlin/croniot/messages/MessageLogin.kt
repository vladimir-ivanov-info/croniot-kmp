package croniot.messages

//import com.google.gson.Gson

data class MessageLogin(val accountEmail: String = "",
                        val accountPassword: String,
                        val deviceUuid: String,
                        val deviceToken: String?)