package com.croniot.android.core.di

import com.croniot.android.app.Global
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {

    private var BASE_URL : String

    init {
        BASE_URL = "http://" + Global.SERVER_ADDRESS + ":" + Global.SERVER_PORT

    }

    val networkModule = module {
        // Provide Retrofit instance
        single {
            Retrofit.Builder()
                .baseUrl(BASE_URL) // Replace with your API base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    fun reloadRetrofitRemote() { //TODO change name later
        val baseUrl = Global.SERVER_ADDRESS_REMOTE
        loadKoinModules(
            module {
                single {
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(get())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
        )
    }

    fun reloadRetrofitLocal() {
        val baseUrl = Global.SERVER_ADDRESS_LOCAL

        loadKoinModules(
            module {
                single {
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(get())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                }
            }
        )
    }
}