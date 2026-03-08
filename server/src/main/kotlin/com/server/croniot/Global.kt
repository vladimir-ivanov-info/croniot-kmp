import com.server.croniot.config.Secrets
import java.util.*

object Global {

    var TESTING = true

    val secrets: Secrets = Secrets.fromEnvironment()

    var startMillis: Long = 0

    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        return uuid.toString().substring(0, length)
    }
}
