package com.croniot.android.core.data.source.remote.retrofit

import ZonedDateTimeAdapter
import com.google.gson.GsonBuilder
import com.croniot.android.app.Global
import com.croniot.android.features.login.data.LoginApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZonedDateTime

object RetrofitClient {

    val gson = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    private var BASE_URL : String

    init {
        BASE_URL = "http://" + Global.SERVER_ADDRESS + ":" + Global.SERVER_PORT
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val loginApiService: LoginApiService = retrofit.create(LoginApiService::class.java)
    val taskConfigurationApiService: TaskConfigurationApiService = retrofit.create(
        TaskConfigurationApiService::class.java)
}