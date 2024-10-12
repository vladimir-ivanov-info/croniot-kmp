package db

//import io.ktor.application.*
import com.google.gson.GsonBuilder
//import commons.models.Result
import configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.EmptyContent.status
import io.ktor.http.*
import io.ktor.server.testing.*

import kotlin.test.*

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
//import messages.MessageRegisterAccount
import module
import kotlin.test.*



//@Serializable
//data class Data(val message: String)

class ApplicationTest{



/*
    @Test
    fun testRoot() = testApplication {
        application {
            //Global.TESTING = false

            module(testing = true)
            //val a = Routing

            //io.ktor.server.routing.Routing.configureRoutingmodule()
           // Application::configureRouting
            // module = Application::configureRouting
           // configureRouting()
        }

       /* client.post("/api/register_account").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello, World!", bodyAsText())
        }*/

        val client = createClient { } //TODO try commenting and running test

        // JSON data to send in the POST request
        //val jsonData = Json.encodeToString(Data("Test data"))

        val messageRegisterAccount = MessageRegisterAccount("androidUuid_1234",  "vladimiriot","email1@gmail.com", "password1")
        val gson = GsonBuilder().setPrettyPrinting().create()
        val messageRegisterAccountJson = gson.toJson(messageRegisterAccount)
        //val jsonData =

        // Send a POST request to the /submit path with JSON data
        val response = client.post("/api/register_account") {
            contentType(ContentType.Application.Json)
            setBody(messageRegisterAccountJson)
        }

        val responseBody = response.bodyAsText()

        val expectedResult = Result(true, "")
        val expectedResultJson = gson.toJson(expectedResult)

        assertEquals(responseBody, expectedResultJson)

//print("$responseBody")
        // Assert the response status and content
        assertEquals(HttpStatusCode.OK, response.status)

    }

    @AfterTest
    fun teardown() {
        // Teardown code here
        //client.close()
        println("${this::class.simpleName}: Teardown completed")
    }*/
}