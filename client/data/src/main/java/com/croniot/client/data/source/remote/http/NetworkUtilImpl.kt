package com.croniot.client.data.source.remote.http

import com.croniot.client.core.ServerConfig
import com.croniot.client.data.repositories.LocalDataRepository
import com.google.gson.Gson
import croniot.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


class NetworkUtilImpl(private val localDataStoreRepository: LocalDataRepository) : NetworkUtil {

    private val client = OkHttpClient.Builder().followRedirects(false).build()

    /*fun resolveServerAddressIfNotExists() {
        val serverAddress = runBlocking {
            //DataStoreController.loadData(DataStoreController.KEY_SERVER_ADDRESS).first()
            localDataStoreRepository.getServerAddress()
        }

        //Comment due to VPS not active.
        //if (serverAddress == null) {
        //    resolveAndFollowRedirects("vladimiriot.com") // TODO make constant in Global. Catch error if can't be resolved
        //} else {
            ServerConfig.SERVER_ADDRESS_REMOTE = serverAddress?: "192.168.50.163"
            ServerConfig.mqttBrokerUrl = "tcp://${ServerConfig.SERVER_ADDRESS_REMOTE}:1883"
        //}
    }

    private fun resolveAndFollowRedirects(domain: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val initialUrl = "http://$domain"
            // val initialUrl = domain
            val finalUrl = followRedirects(initialUrl)
            val ipAddress = resolveIpAddress(finalUrl)

            val addresses = ipAddress.split("\n")

            if (addresses.size > 0) {
                val ipv4Address = addresses[0]

               // DataStoreController.saveData(DataStoreController.KEY_SERVER_ADDRESS, ipv4Address)
                localDataStoreRepository.saveServerAddress(ipv4Address)


                ServerConfig.SERVER_ADDRESS = ipv4Address
                ServerConfig.mqttBrokerUrl = "tcp://${ServerConfig.SERVER_ADDRESS_REMOTE}:1883"
            }
        }
    }

    private suspend fun followRedirects(url: String): String = withContext(Dispatchers.IO) {
        var currentUrl = url
        var redirect = true
        var previousResponse: Response? = null

        while (redirect) {
            val request = Request.Builder().url(currentUrl).build()
            val response = client.newCall(request).execute()
            previousResponse?.close()

            if (response.isRedirect) {
                currentUrl = response.header("Location") ?: currentUrl
                if (!currentUrl.startsWith("http")) {
                    val uri = URI(url)
                    currentUrl = uri.resolve(currentUrl).toString()
                }
            } else {
                redirect = false
            }

            previousResponse = response
        }

        currentUrl
    }

    private suspend fun resolveIpAddress(url: String): String = withContext(Dispatchers.IO) {
        try {
            val uri = URI(url)
            val host = uri.host ?: return@withContext "Invalid URL"
            val addresses = InetAddress.getAllByName(host)
            addresses.joinToString(separator = "\n") { it.hostAddress }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown host"
        }
    }*/






    suspend fun post(endPoint: String, postData: String): Result {
        val url = "http://" + ServerConfig.SERVER_ADDRESS + ":" + ServerConfig.SERVER_PORT + endPoint
        return withContext(Dispatchers.IO) {
            performPostRequest(url, postData)
        }
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
