package com.croniot.client.data.source.remote

import com.croniot.client.core.ServerConfig
import com.croniot.client.data.source.remote.http.login.LoginApi
import okhttp3.OkHttpClient
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient()) // Usa el cliente por defecto, o injecta uno si lo necesitas
            //.client(get()) // Usa el cliente por defecto, o injecta uno si lo necesitas
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val networkModule = module {

        single { HostSelectionInterceptor() }

        single {
            OkHttpClient.Builder()
                .addInterceptor(get<HostSelectionInterceptor>())
                .build()
        }

        single {
            //buildRetrofit("http://${ServerConfig.SERVER_ADDRESS}:${ServerConfig.SERVER_PORT}")

            Retrofit.Builder()
                .baseUrl("http://${ServerConfig.SERVER_ADDRESS}:${ServerConfig.SERVER_PORT}/") // Debe tener protocolo, host y terminar en `/`
                .client(get())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        single<LoginApi> { get<Retrofit>().create(LoginApi::class.java) }

    }



    fun reloadRetrofit(newServerAddress: String) {
        val fullUrl = "http://$newServerAddress:${ServerConfig.SERVER_PORT}"
        loadKoinModules(
            module {
                single<Retrofit> {
                    buildRetrofit(fullUrl)
                }
            }
        )
    }
}