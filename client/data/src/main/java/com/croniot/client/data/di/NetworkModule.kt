package com.croniot.client.data.di

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
                        .add("192.168.50.163", "sha256/Znxl+y8ZODYcvOarE+j2P9ppHbnBZVDwyrN9xQ/sjuI=")
                        .build()
                )
                .addInterceptor(get<HostSelectionInterceptor>())
                .build()
        }

        single<Retrofit> {
            val contentType = "application/json".toMediaType()
            Retrofit.Builder()
                .baseUrl("https://192.168.50.163:8443/")
                .client(get())
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
        }

        single<LoginApi> { get<Retrofit>().create(LoginApi::class.java) }
        single<TaskConfigurationApiService> { get<Retrofit>().create(TaskConfigurationApiService::class.java) }
    }
}
