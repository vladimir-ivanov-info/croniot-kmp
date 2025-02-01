package com.croniot.android.core.util

import com.croniot.android.app.Global
import com.croniot.android.core.data.source.local.DataStoreController
//import com.croniot.android.core.data.source.local.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.InetAddress
import java.net.URI

object NetworkUtil {

    private val client = OkHttpClient.Builder().followRedirects(false).build()

    fun resolveServerAddressIfNotExists(){
        val serverAddress = runBlocking {
            DataStoreController.loadData(DataStoreController.KEY_SERVER_ADDRESS).first()
        }

        if(serverAddress == null){
            resolveAndFollowRedirects("vladimiriot.com") //TODO make constant in Global. Catch error if can't be resolved
        } else {
            Global.SERVER_ADDRESS_REMOTE = serverAddress
            Global.mqttBrokerUrl = "tcp://${Global.SERVER_ADDRESS_REMOTE}:1883"
        }
    }

    private fun resolveAndFollowRedirects(domain: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val initialUrl = "http://$domain"
            //val initialUrl = domain
            val finalUrl = followRedirects(initialUrl)
            val ipAddress = resolveIpAddress(finalUrl)

            val addresses = ipAddress.split("\n")

            if(addresses.size > 0){
                val ipv4Address = addresses[0]

                DataStoreController.saveData(DataStoreController.KEY_SERVER_ADDRESS, ipv4Address)

                Global.SERVER_ADDRESS = ipv4Address
                Global.mqttBrokerUrl = "tcp://${Global.SERVER_ADDRESS_REMOTE}:1883"
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
    }

}