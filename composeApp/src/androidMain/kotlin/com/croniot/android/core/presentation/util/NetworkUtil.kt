package com.croniot.android.core.presentation.util

import com.croniot.android.app.Global.SERVER_ADDRESS
import com.croniot.android.app.Global.SERVER_PORT
import com.google.gson.Gson
import croniot.models.Result
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object NetworkUtil {

    fun performPostRequestToEndpoint(endPoint: String, postData: String) : Result {
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