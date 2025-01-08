package com.croniot.android.app

import android.app.Application
import com.croniot.android.core.di.DependencyInjectionModule
import com.croniot.android.features.registeraccount.di.RegisterAccountModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            //modules(DependencyInjectionModule.dependencyInjectionModule)
            modules(
                DependencyInjectionModule.dependencyInjectionModule,
                RegisterAccountModule.registerAccountModule
            )
        }
    }

}
