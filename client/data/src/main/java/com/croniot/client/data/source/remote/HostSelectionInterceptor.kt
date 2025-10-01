package com.croniot.client.data.source.remote

//TODO moverl la clase al sitio correcto

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HostSelectionInterceptor : Interceptor {

    // Host mutable (sin protocolo ni puerto)
    @Volatile
    var host: String? = null

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentHost = host

        if (currentHost != null) {
            val newUrl = originalRequest.url.newBuilder()
                .host(currentHost)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            return chain.proceed(newRequest)
        }

        return chain.proceed(originalRequest)
    }
}
