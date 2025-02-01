package com.croniot.android.app

import android.app.Application
import com.croniot.android.core.data.entities.AccountEntity
import com.croniot.android.core.data.entities.DeviceEntity
import com.croniot.android.core.data.entities.KeyValueEntity
import com.croniot.android.core.data.entities.ParameterEntity
import com.croniot.android.core.data.entities.ParameterSensorEntity
import com.croniot.android.core.data.entities.ParameterTaskEntity
import com.croniot.android.core.data.entities.SensorDataEntity
import com.croniot.android.core.data.entities.SensorTypeEntity
import com.croniot.android.core.data.entities.TaskEntity
import com.croniot.android.core.data.entities.TaskStateInfoEntity
import com.croniot.android.core.data.entities.TaskTypeEntity
import com.croniot.android.core.di.DependencyInjectionModule
import com.croniot.android.core.di.NetworkModule
import com.croniot.android.features.device.features.sensors.di.SensorsModule
import com.croniot.android.features.registeraccount.di.RegisterAccountModule
import com.croniot.android.features.login.di.LoginModule
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.Realm
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {

    lateinit var realm: Realm

    override fun onCreate() {
        super.onCreate()

        val config = RealmConfiguration.Builder(
            schema = setOf(
                AccountEntity::class,
                DeviceEntity::class,
                SensorTypeEntity::class,
                TaskTypeEntity::class,
                ParameterEntity::class,
                ParameterSensorEntity::class,
                ParameterTaskEntity::class,
                SensorDataEntity::class,
                TaskEntity::class,
                TaskStateInfoEntity::class,
                KeyValueEntity::class
            )
        )
        .name("croniot.realm")
        .schemaVersion(1)
        .build()

        realm = Realm.open(config)

        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            modules(
                DependencyInjectionModule.dependencyInjectionModule,
                NetworkModule.networkModule,
                RegisterAccountModule.registerAccountModule,
                LoginModule.loginModule,
                SensorsModule.sensorsModule
            )
        }
    }
}