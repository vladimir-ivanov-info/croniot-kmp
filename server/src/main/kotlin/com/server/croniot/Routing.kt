import com.google.gson.GsonBuilder
import croniot.models.Result
import com.croniot.server.db.AccountController
import com.croniot.server.db.RegisterAccountController
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.croniot.server.login.AuthenticationController
import croniot.messages.*
import java.time.ZonedDateTime


fun Application.configureRouting() {

    routing {

        post("/api/login") {
            val message = call.receiveText();
            val messageLogin = MessageFactory.fromJson<MessageLogin>(message)
            val loginResult = AuthenticationController.tryLogin(messageLogin)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(loginResult)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }

        post("/api/iot/login") {
            val message = call.receiveText();
            val messageLogin = MessageFactory.fromJson<MessageLogin>(message)
            val loginIotResult = AuthenticationController.tryLoginIoT(messageLogin)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(loginIotResult)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }

        get("/taskConfiguration/{deviceUuid}") {
            val deviceUuid = call.parameters["deviceUuid"] ?: return@get call.respondText(
                "Missing or malformed deviceUuid",
                status = HttpStatusCode.BadRequest
            )

            // Assuming you have a function to get the configurations by UUID
            val taskConfigurations = TaskController.getTasksByDeviceUuid(deviceUuid)
            // You need to add a response here, assuming taskConfigurations is properly fetched
            if (taskConfigurations.isNotEmpty()) {
                call.respond(taskConfigurations)
            } else {
                call.respond(HttpStatusCode.NotFound, "No configurations found for UUID: $deviceUuid")
            }
        }

        post("/api/iot/{deviceUuid}/task_type/{taskTypeUid}/{taskUid}/state/") {
            val deviceUuid = call.parameters["deviceUuid"]
            val taskTypeUid = call.parameters["taskTypeUid"]
            val taskUid = call.parameters["taskUid"]
            val receivedData = call.receive<String>()
            println("Received: $receivedData for device: $deviceUuid, task type: $taskTypeUid, task: $taskUid")
            if(deviceUuid != null && taskTypeUid != null && taskUid != null){
                TaskController.addTaskState(deviceUuid, taskTypeUid.toLong(), taskUid.toLong(), receivedData)
            }

        }

        post("/api/register_account") {
            //            val data = call.receive<AccountAndDevice>() TODO try this instead of json
            val message = call.receiveText();
            val messageRegisterAccount = MessageFactory.fromJson<MessageRegisterAccount>(message)
            val lResult = RegisterAccountController.registerAccount(messageRegisterAccount)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(lResult)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }

        post("/api/register_client"){
            val message = call.receiveText();
            val messageRegisterDevice = MessageFactory.fromJson<MessageRegisterDevice>(message)
            val lResult = RegisterAccountController.registerDevice(messageRegisterDevice)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(lResult)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }

        post("/api/register_sensor"){
            val message = call.receiveText();
            val messageRegisterSensor = MessageFactory.fromJson<MessageRegisterSensor>(message)
            val result = RegisterSensorController.registerSensor(messageRegisterSensor)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }

        post("/api/register_task_type"){
            val message = call.receiveText();

            val gson = GsonBuilder()
                .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeSerializer())
                .create()
            val messageRegisterTask = gson.fromJson(message, MessageRegisterTaskType::class.java)

            val result = RegisterTaskController.registerTask(messageRegisterTask)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }

        post("/api/add_task"){
            val message = call.receiveText();

            val messageAddTask = MessageFactory.fromJson<MessageAddTask>(message)

            print(messageAddTask.toString())

            val result = AddTaskController.registerTask(messageAddTask)
            val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(result)
            call.respondText(responseJSON, ContentType.Text.Plain)
        }


        post("/api/account_info") {
            val millisStart = System.currentTimeMillis()

            val message = call.receiveText();
            val messageGetAcountInfo = MessageFactory.fromJson<MessageGetAccountInfo>(message)
            val token = messageGetAcountInfo.token
            val device = AuthenticationController.getDeviceAssociatedWithToken(token) //TODO test for when the device is contained in multiple accounts

            if(device != null){
                val accounts = AccountController.getAccountOfDevice(device)

                if(!accounts.isEmpty()){
                    val account = accounts.first()
                    val accountJson = GsonBuilder().setPrettyPrinting().create().toJson(account)
                    val result = Result(true, accountJson)
                    val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(result)
                    call.respondText(responseJSON, ContentType.Text.Plain)

                }  else {
                    val result = Result(true, "Could not get account.")
                    val responseJSON = GsonBuilder().setPrettyPrinting().create().toJson(result)
                    call.respondText(responseJSON, ContentType.Text.Plain)
                }
                println()
            }
        }
    }
}