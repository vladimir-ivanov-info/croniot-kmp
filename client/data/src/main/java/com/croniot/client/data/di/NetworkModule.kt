package com.croniot.client.data.di

import com.croniot.client.data.source.remote.http.HostSelectionInterceptor
import com.croniot.client.data.source.remote.http.TaskConfigurationApiService
import com.croniot.client.data.source.remote.http.login.LoginApi
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    val networkModule = module {

        single { HostSelectionInterceptor() }

        single<OkHttpClient> {
            OkHttpClient.Builder()
                .certificatePinner(
                    CertificatePinner.Builder()
                        .add("192.168.50.163", "sha256/Znxl+y8ZODYcvOarE+j2P9ppHbnBZVDwyrN9xQ/sjuI=")
                        //.add("192.168.50.163", "sha256/Xnxl+y8ZODYcvOarE+j2P9ppHbnBZVDwyrN9xQ/sjuI=")
                        .build()
                )
                .addInterceptor(get<HostSelectionInterceptor>())
                .build()
        }

        single<Retrofit> {
            // buildRetrofit("http://${ServerConfig.SERVER_ADDRESS}:${ServerConfig.SERVER_PORT}")

            Retrofit.Builder()
                //.baseUrl("http://${ServerConfig.SERVER_ADDRESS}:${ServerConfig.SERVER_PORT}/") // Debe tener protocolo, host y terminar en `/`
                .baseUrl("https://${"192.168.50.163"}:${8443}/") // Debe tener protocolo, host y terminar en `/`
                .client(get())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        single<LoginApi> { get<Retrofit>().create(LoginApi::class.java) }
        single<TaskConfigurationApiService> { get<Retrofit>().create(TaskConfigurationApiService::class.java) }
    }
}
