package com.croniot.android

import com.google.gson.Gson
import croniot.models.Result
import croniot.models.dto.AccountDto
import croniot.models.dto.DeviceDto
import croniot.models.dto.TaskTypeDto
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object Global {

    val appName = "croniot"

    val SERVER_ADDRESS_LOCAL = "192.168.50.163"
    var SERVER_ADDRESS_REMOTE = ""

    var SERVER_ADDRESS = "192.168.50.163"
    //var SERVER_ADDRESS = "51.77.195.204"
    var SERVER_PORT = 8090

    var mqttBrokerUrl = "tcp://51.77.195.204:1883"
    val mqttClientId = "AndroidMQTTClient"

    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        val uniqueString = uuid.toString().substring(0, length)
        return uniqueString
    }

    var selectedDevice : DeviceDto? = null
    var selectedTaskType: TaskTypeDto? = null //TODO persist

    fun performPostRequestToEndpoint(endPoint: String, postData: String) : Result{
        val url =  "http://" + SERVER_ADDRESS + ":" + SERVER_PORT + endPoint
        return performPostRequest(url, postData)
    }

    fun performPostRequest(urlString: String, postData: String): Result {

        var result: Result

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.doOutput = true
            connection.connectTimeout = 5000
            val outputStream: OutputStream = connection.outputStream
            outputStream.write(postData.toByteArray(Charsets.UTF_8))

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val responseMessage = response.toString()

                result = Gson().fromJson(responseMessage, Result::class.java)
            } else {
                result = Result(false, "$responseCode")
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
            println(e.message)
            result = Result(false, "$e.message")
        }

        return result
    }

}