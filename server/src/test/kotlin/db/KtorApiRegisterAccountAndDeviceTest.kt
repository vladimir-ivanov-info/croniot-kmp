package db

// import io.ktor.application.*
import com.google.gson.GsonBuilder
import croniot.messages.MessageRegisterAccount
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
// import messages.MessageRegisterAccountAndDevice
import com.server.croniot.application.module
import kotlin.test.*

class KtorApiRegisterAccountAndDeviceTest {

    @Test
    fun testRegisterAccountAndDevice() = testApplication {
        application {
            module()
        }

        val accountUuid = "test_uuid"
        val nickName = "test_nickname"
        val email = "test_email@gmail.com"
        val password = "test_password"

        val response = client.post("/api/register_account_and_device") {
            contentType(ContentType.Application.Json)
            // setBody("""{"accountId":"12345", "deviceId":"abcde"}""")

            /*val email = "email1@gmail.com"
            val password = "password1"
            val accountUuid = "account1Uuid"
            val deviceUuid = "android1Uuid"
            val deviceName = "Vladimir's Android"
            val deviceDescription = "Vladimir's Android device used to monitor and control his IoT devices."

            val message = MessageRegisterAccountAndDevice(accountUuid, email, password, deviceUuid, deviceName, deviceDescription)
            val gson = GsonBuilder().setPrettyPrinting().create()
            val messageRegisterAccountAndDeviceJson = gson.toJson(message)
            setBody(messageRegisterAccountAndDeviceJson)*/

            val gson = GsonBuilder().setPrettyPrinting().create()

            val message = MessageRegisterAccount(accountUuid, nickName, email, password)
            val messageRegisterAccountJson = gson.toJson(message)
            setBody(messageRegisterAccountJson)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Account $accountUuid registered successfully!", response.bodyAsText())
    }
}
