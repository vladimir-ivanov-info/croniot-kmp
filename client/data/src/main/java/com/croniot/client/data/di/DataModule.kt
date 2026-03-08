package com.croniot.client.data.di

import com.croniot.client.data.repositories.SensorDataRepositoryImpl
import com.croniot.client.data.repositories.TaskTypesRepositoryImpl
import com.croniot.client.data.source.sensors.LocalSensorDataSource
import com.croniot.client.data.source.sensors.LocalSensorDataSourceRoomImpl
import com.croniot.client.data.source.sensors.RemoteSensorDataSource
import com.croniot.client.data.source.sensors.RemoteSensorDataSourceImpl
import com.croniot.client.domain.repositories.AccountRepository
import com.croniot.client.data.repositories.AccountRepositoryImpl
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.AuthRepository
import com.croniot.client.data.repositories.AuthRepositoryImpl
import com.croniot.client.domain.repositories.SessionRepository
import com.croniot.client.data.repositories.SessionRepositoryImpl
import com.croniot.client.data.source.remote.mqtt.TasksDataSource
import com.croniot.client.data.source.remote.http.NetworkUtil
import com.croniot.client.data.source.remote.http.NetworkUtilImpl
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImpl
import com.croniot.client.data.source.remote.mqtt.TasksDataSourceImpl
import androidx.room.Room
import com.croniot.client.data.source.local.room.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {

    single<AppDatabase> {
        Room.databaseBuilder(
            context = androidContext(),
            klass = AppDatabase::class.java,
            name = "croniot.db",
        ).build()
    }

    single { get<AppDatabase>().sensorDataDao() }

    single<NetworkUtil> { NetworkUtilImpl() }

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
        RemoteSensorDataSourceImpl()
    }

    single<SensorDataRepository> {
        SensorDataRepositoryImpl(
            remoteSensorDataSource = get(),
            localSensorDataSource = get(),
        )
    }
}
