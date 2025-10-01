package com.croniot.android.app

import android.app.Application
import com.croniot.android.core.data.entities.AccountEntity
import com.croniot.android.core.data.entities.DeviceEntity
import com.croniot.android.core.data.entities.KeyValueEntity
import com.croniot.android.core.data.entities.ParameterEntity
import com.croniot.android.core.data.entities.ParameterSensorEntity
import com.croniot.android.core.data.entities.ParameterTaskEntity
import com.croniot.android.core.data.entities.SensorDataEntity
import com.croniot.android.core.data.entities.SensorDataRealm
import com.croniot.android.core.data.entities.SensorTypeEntity
import com.croniot.android.core.data.entities.TaskRealm
import com.croniot.android.core.data.entities.TaskStateInfoRealm
import com.croniot.android.core.data.entities.TaskTypeEntity
import com.croniot.android.core.di.MainDIModule
import com.croniot.android.features.registeraccount.di.RegisterAccountModule
import com.croniot.client.data.di.dataModule
import com.croniot.client.data.source.local.RealmRef
import com.croniot.client.data.source.remote.NetworkModule
import com.croniot.client.domain.di.domainDiModule
import com.croniot.client.features.login.di.LoginModule
import com.croniot.client.features.sensors.di.SensorsModule
import com.croniot.client.features.tasktypes.di.taskTypeModule
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApp : Application() {

    companion object {
        lateinit var realm: Realm // TODO make global in another class
    }

    suspend fun clearDatabase() {
        realm.write {
            deleteAll()
        }
    }

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
                TaskRealm::class,
                TaskStateInfoRealm::class,
                KeyValueEntity::class,
                SensorDataRealm::class,
            ),
        )
            .name("croniot.realm")
            .schemaVersion(4)
            .deleteRealmIfMigrationNeeded() // Automatically migrate (not recommended for production)
            .build()

        realm = Realm.open(config)

        RealmRef.realmRef = realm

        /*GlobalScope.launch(Dispatchers.IO) {
            clearDatabase()
        }*/

        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            allowOverride(true)
            modules(
                MainDIModule.mainDIModule,
                NetworkModule.networkModule,
                RegisterAccountModule.registerAccountModule,
                LoginModule.loginModule,
                SensorsModule.sensorsModule,
                dataModule,
                domainDiModule,

                taskTypeModule,
            )
        }
    }
}
