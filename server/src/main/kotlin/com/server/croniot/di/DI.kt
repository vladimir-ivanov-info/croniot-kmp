package com.server.croniot.di

import com.server.croniot.application.AppComponent
import com.server.croniot.application.DaggerAppComponent

object DI {
    val appComponent: AppComponent by lazy {
        DaggerAppComponent.create()
    }
}