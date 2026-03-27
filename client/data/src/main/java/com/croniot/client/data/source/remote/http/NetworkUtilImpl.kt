package com.croniot.client.data.source.remote.http

import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.local.LocalDatasource
import croniot.messages.MessageFactory
import croniot.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class NetworkUtilImpl(
    private val localDatasource: LocalDatasource,
) : NetworkUtil {

    override suspend fun post(endPoint: String, postData: String): Result {
        val ip = localDatasource.getServerIp().first() ?: ServerConfig.SERVER_IP_REMOTE
        val url = "https://${ip}:${ServerConfig.SERVER_PORT}$endPoint"
        return withContext(Dispatchers.IO) {
            performPostRequest(url, postData)
        }
    }

    private fun performPostRequest(urlString: String, postData: String): Result {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                doOutput = true
                connectTimeout = 5_000
                readTimeout = 10_000
            }

            connection.outputStream.use { os ->
                os.write(postData.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                MessageFactory.fromJson<Result>(responseBody)
            } else {
                Result(false, "HTTP $responseCode")
            }
        } catch (e: Exception) {
            Result(false, e.message ?: "Unknown error")
        } finally {
            connection?.disconnect()
        }
    }
}