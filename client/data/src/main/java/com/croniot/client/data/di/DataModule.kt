package com.croniot.client.data.di

import com.croniot.client.data.repositories.AccountRepository
import com.croniot.client.data.repositories.AccountRepositoryImpl
import com.croniot.client.data.repositories.AuthRepository
import com.croniot.client.data.repositories.AuthRepositoryImpl
import com.croniot.client.data.repositories.SessionRepository
import com.croniot.client.data.repositories.SessionRepositoryImpl
import com.croniot.client.data.source.remote.TasksDataSource
import com.croniot.client.data.source.remote.http.NetworkUtilImpl
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImpl
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImplDemo
import com.croniot.client.data.source.remote.http.login.StrategyLoginDataSource
import com.croniot.client.data.source.remote.mqtt.TasksDataSourceImpl
import com.croniot.client.data.strategy.DataSourceStrategy
import com.croniot.client.data.strategy.DataSourceStrategyBus
import com.croniot.client.data.strategy.MutableDataSourceStrategyBus
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule = module {

    single { NetworkUtilImpl(localDataStoreRepository = get()) }

    single<AuthRepository> { AuthRepositoryImpl(loginDataSource = get()) }

    single<AccountRepository> { AccountRepositoryImpl(localDataSource = get()) }

    single<SessionRepository> {
        SessionRepositoryImpl(
            localDataSource = get(),
        )
    }

    single<TasksDataSource> { TasksDataSourceImpl() }

    // LOGIN LOGIN LOGIN LOGIN LOGIN
    single<DataSourceStrategyBus> { MutableDataSourceStrategyBus(DataSourceStrategy.REAL) } // TODO
    single<LoginDataSource>(qualifier = named("real")) { LoginDataSourceImpl(api = get()) }
    single<LoginDataSource>(qualifier = named("demo")) { LoginDataSourceImplDemo() }

    single<LoginDataSource> {
        StrategyLoginDataSource(
            realLoginDataSource = get(named("real")),
            demoLoginDataSource = get(named("demo")),
            bus = get(),
        )
    }
    single<AuthRepository> { AuthRepositoryImpl(get()) }

    // SENSORS SENSORS SENSORS SENSORS SENSORS
}
