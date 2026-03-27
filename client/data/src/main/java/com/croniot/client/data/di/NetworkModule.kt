package com.croniot.client.data.di

import com.croniot.client.core.config.ServerConfig
import com.croniot.client.data.source.remote.http.HostSelectionInterceptor
import com.croniot.client.data.source.remote.http.TaskConfigurationApiService
import com.croniot.client.data.source.remote.http.login.LoginApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val networkModule = module {

        single { HostSelectionInterceptor() }

        single<OkHttpClient> {
            OkHttpClient.Builder()
                .certificatePinner(
                    CertificatePinner.Builder()
                        .add(ServerConfig.SERVER_IP_LOCAL, ServerConfig.CERT_PIN_SHA256)
                        .add(ServerConfig.SERVER_IP_REMOTE, ServerConfig.CERT_PIN_SHA256)
                        .build()
                )
                .addInterceptor(get<HostSelectionInterceptor>())
                .build()
        }

        single<Retrofit> {
            val contentType = "application/json".toMediaType()
            Retrofit.Builder()
                //.baseUrl("https://${ServerConfig.SERVER_IP_LOCAL}:${ServerConfig.SERVER_PORT}/")
                .baseUrl("https://${ServerConfig.SERVER_IP_REMOTE}:${ServerConfig.SERVER_PORT}/")
                .client(get())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
        }

        single<LoginApi> {
            get<Retrofit>().create(LoginApi::class.java)
        }

        single<TaskConfigurationApiService> {
            get<Retrofit>().create(TaskConfigurationApiService::class.java)
        }
    }
}