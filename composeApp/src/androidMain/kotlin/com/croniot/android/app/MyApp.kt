package com.croniot.android.app

import android.app.Application
import android.os.StrictMode
import com.croniot.android.BuildConfig
import com.croniot.android.core.di.MainDIModule
import com.croniot.android.features.registeraccount.di.RegisterAccountModule
import com.croniot.client.data.di.NetworkModule
import com.croniot.client.data.di.dataModule
import com.croniot.client.domain.di.domainDiModule
import com.croniot.client.features.blediscovery.di.BleDiscoveryModule
import com.croniot.client.features.login.di.LoginModule
import com.croniot.client.features.sensors.di.SensorsModule
import com.croniot.client.features.taskhistory.di.TaskHistoryModule
import com.croniot.client.features.tasktypes.di.TaskTypesModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Initialize Koin in the background to avoid UI jank and StrictMode violations
        // Note: If your first Activity depends on Koin immediately, ensure it handles potential delays.
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            startKoin {
                androidLogger()
                androidContext(this@MyApp)
                allowOverride(true)
                modules(
                    MainDIModule.mainDIModule,
                    NetworkModule.networkModule,
                    RegisterAccountModule.registerAccountModule,
                    LoginModule.loginModule,
                    BleDiscoveryModule.bleDiscoveryModule,
                    SensorsModule.sensorsModule,
                    dataModule,
                    domainDiModule,
                    TaskTypesModule.taskTypesModule,
                    TaskHistoryModule.taskHistoryModule,
                )
            }
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
