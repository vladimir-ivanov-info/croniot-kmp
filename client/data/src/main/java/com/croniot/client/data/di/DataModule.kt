package com.croniot.client.data.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.croniot.client.data.repositories.AccountRepositoryImpl
import com.croniot.client.data.repositories.AuthRepositoryImpl
import com.croniot.client.data.repositories.SensorDataRepositoryImpl
import com.croniot.client.data.repositories.SessionRepositoryImpl
import com.croniot.client.data.repositories.TaskTypesRepositoryImpl
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.remote.http.NetworkUtil
import com.croniot.client.data.source.remote.http.NetworkUtilImpl
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImpl
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.data.source.remote.mqtt.TasksDataSourceImpl
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.LocalSensorDataSourceRoomImpl
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSourceImpl
import com.croniot.client.data.source.taskhistory.LocalTaskHistoryDataSource
import com.croniot.client.data.source.taskhistory.LocalTaskHistoryDataSourceRoomImpl
import com.croniot.client.domain.repositories.AccountRepository
import com.croniot.client.domain.repositories.AuthRepository
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.SessionRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_history_cache` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `deviceUuid` TEXT NOT NULL,
                `stateInfoId` INTEGER,
                `taskUid` INTEGER NOT NULL,
                `taskTypeUid` INTEGER NOT NULL,
                `timeStampMillis` INTEGER NOT NULL,
                `state` TEXT NOT NULL,
                `progress` REAL NOT NULL,
                `errorMessage` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid` ON `task_history_cache` (`deviceUuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid_timeStampMillis` ON `task_history_cache` (`deviceUuid`, `timeStampMillis`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_history_cache_deviceUuid_stateInfoId` ON `task_history_cache` (`deviceUuid`, `stateInfoId`)")
    }
}

val dataModule = module {

    single(named("appScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    single<AppDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = "croniot.db",
        ).addMigrations(MIGRATION_1_2).build()
    }

    single { get<AppDatabase>().sensorDataDao() }
    single { get<AppDatabase>().taskHistoryCacheDao() }

    single<NetworkUtil> { NetworkUtilImpl(localDatasource = get()) }

    single<AuthRepository> { AuthRepositoryImpl(loginDataSource = get()) }

    single<AccountRepository> { AccountRepositoryImpl(localDataSource = get()) }

    single<SessionRepository> {
        SessionRepositoryImpl(
            localDataSource = get(),
        )
    }

    single<TasksDataSource> {
        TasksDataSourceImpl(
            networkUtil = get(),
            taskConfigurationApiService = get(),
            localDatasource = get(),
            appScope = get(named("appScope")),
        )
    }

    single<LoginDataSource> {
        LoginDataSourceImpl(api = get())
    }
    single<AuthRepository> { AuthRepositoryImpl(get()) }

    single<TaskTypesRepository> { TaskTypesRepositoryImpl() }

    single<LocalSensorDataSource> {
        LocalSensorDataSourceRoomImpl(sensorDataDao = get())
    }

    single<RemoteSensorDataSource> {
        RemoteSensorDataSourceImpl(
            appScope = get(named("appScope")),
            localDatasource = get(),
        )
    }

    single<LocalTaskHistoryDataSource> {
        LocalTaskHistoryDataSourceRoomImpl(
            taskHistoryCacheDao = get(),
        )
    }

    single<SensorDataRepository> {
        SensorDataRepositoryImpl(
            remoteSensorDataSource = get(),
            localSensorDataSource = get(),
            scope = get(named("appScope")),
        )
    }
}
