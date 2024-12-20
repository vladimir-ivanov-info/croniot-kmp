package com.croniot.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.InetAddress
import java.net.URI

object NetworkUtil {

    private val client = OkHttpClient.Builder().followRedirects(false).build()

    fun resolveServerAddressIfNotExists(){
        val serverAddress = SharedPreferences.loadData(SharedPreferences.KEY_SERVER_ADDRESS)
        if(serverAddress == null){
            resolveAndFollowRedirects("vladimiriot.com") //TODO make constant in Global. Catch error if can't be resolved
        } else {
            Global.SERVER_ADDRESS_REMOTE = serverAddress
            Global.mqttBrokerUrl = "tcp://${Global.SERVER_ADDRESS_REMOTE}:1883"
        }
    }

    fun resolveAndFollowRedirects(domain: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val initialUrl = "http://$domain"
            //val initialUrl = domain
            val finalUrl = NetworkUtil.followRedirects(initialUrl)
            val ipAddress = NetworkUtil.resolveIpAddress(finalUrl)

            val addresses = ipAddress.split("\n")

            if(addresses.size > 0){
                val ipv4Address = addresses[0]
                SharedPreferences.saveData(SharedPreferences.KEY_SERVER_ADDRESS, ipv4Address)
                Global.SERVER_ADDRESS = ipv4Address
                Global.mqttBrokerUrl = "tcp://${Global.SERVER_ADDRESS_REMOTE}:1883"
            }
        }
    }

    suspend fun followRedirects(url: String): String = withContext(Dispatchers.IO) {
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

    suspend fun resolveIpAddress(url: String): String = withContext(Dispatchers.IO) {
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