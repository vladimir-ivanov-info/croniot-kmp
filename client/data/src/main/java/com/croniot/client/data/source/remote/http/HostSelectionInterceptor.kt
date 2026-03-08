package com.croniot.client.data.source.remote.http

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class HostSelectionInterceptor : Interceptor {

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
