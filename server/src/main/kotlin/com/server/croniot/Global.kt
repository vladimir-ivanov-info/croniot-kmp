import com.google.gson.Gson
import org.eclipse.paho.client.mqttv3.MqttClient
import java.io.File
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object Global {

    var TESTING = true

    val secrets : Secrets

    var startMillis  : Long = 0

    init{
        var secretsFile : File
        //secretsFile = File("")
        //secretsFile.absolutePath
        secretsFile = File(System.getProperty("user.dir"))
        var parent = secretsFile.parentFile

        //secretsFile  = parent.
        secretsFile.listFiles()
        if(TESTING){
            //secretsFile = File("../secrets/secrets_testing.json")
            secretsFile = File(secretsFile.absolutePath + File.separator+ "secrets/secrets_testing.json")
        } else {
            //secretsFile = File("/secrets/secrets.json");
            secretsFile = File(secretsFile.absolutePath + File.separator+ "secrets/secrets.json")

        }


        val jsonString = secretsFile.readText()

        secrets = Gson().fromJson(jsonString, Secrets::class.java)

        println()
    }

    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        val uniqueString = uuid.toString().substring(0, length)
        return uniqueString
    }


    fun getCurrentDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return currentDateTime.format(formatter)
    }
}