package com.croniot.android.core.di

import com.croniot.android.core.constants.ServerConfig
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    private var BASE_URL: String

    init {
        BASE_URL = "http://" + ServerConfig.SERVER_ADDRESS + ":" + ServerConfig.SERVER_PORT
    }

    val networkModule = module {
        single {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    // TODO observe DataStore
    fun reloadRetrofitRemote() { // TODO change name later
        val baseUrl = ServerConfig.SERVER_ADDRESS_REMOTE
        loadKoinModules(
            module {
                single {
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(get())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            },
        )
    }

    // TODO observe DataStore
    fun reloadRetrofitLocal() {
        val baseUrl = ServerConfig.SERVER_ADDRESS_LOCAL
        loadKoinModules(
            module {
                single {
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(get())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            },
        )
    }
}
