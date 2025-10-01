package com.croniot.client.features.sensors.di

import com.croniot.client.data.sensors.datasource.LocalSensorDataSource
import com.croniot.client.data.sensors.datasource.RemoteSensorDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSource
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImpl
import com.croniot.client.data.source.remote.http.login.LoginDataSourceImplDemo
import com.croniot.client.data.source.remote.http.login.StrategyLoginDataSource
import com.croniot.client.data.source.remote.http.sensors.StrategyLocalSensorDataSource
import com.croniot.client.data.source.remote.http.sensors.StrategyRemoteSensorDataSource
import com.croniot.client.data.strategy.DataSourceStrategy
import com.croniot.client.data.strategy.DataSourceStrategyBus
import com.croniot.client.data.strategy.MutableDataSourceStrategyBus
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.features.sensors.data.datasource.local.LocalSensorDataSourceImpl
import com.croniot.client.features.sensors.data.datasource.local.LocalSensorDataSourceImplDemo
import com.croniot.client.features.sensors.data.datasource.remote.RemoteSensorDataSourceImpl
import com.croniot.client.features.sensors.data.datasource.remote.RemoteSensorDataSourceImplDemo
import org.koin.dsl.module
import com.croniot.client.features.sensors.domain.repository.SensorDataRepositoryImpl
import org.koin.core.qualifier.named

object SensorsModule {



    val sensorsModule = module {

        //single<RemoteSensorDataSource> {}

        //single<DataSourceStrategyBus> { MutableDataSourceStrategyBus(DataSourceStrategy.DEMO) }
        single<RemoteSensorDataSource>(qualifier = named("real")) { RemoteSensorDataSourceImpl() }
        single<RemoteSensorDataSource>(qualifier = named("demo")) { RemoteSensorDataSourceImplDemo() }

        single<LocalSensorDataSource>(qualifier = named("real")) { LocalSensorDataSourceImpl() }
        single<LocalSensorDataSource>(qualifier = named("demo")) { LocalSensorDataSourceImplDemo() }

        single<SensorDataRepository> {
            SensorDataRepositoryImpl(
                remoteSensorDataSource = get(),
                localSensorDataSource = get()
            )
        }

        single<LocalSensorDataSource> {
            StrategyLocalSensorDataSource(
                realLocalSensorDataSource = get(named("real")),
                demoLocalSensorDataSource = get(named("demo")),
                bus = get()
            )
        }

        single<RemoteSensorDataSource> {
            StrategyRemoteSensorDataSource(
                realRemoteSensorDataSource = get(named("real")),
                demoRemoteSensorDataSource = get(named("demo")),
                bus = get()
            )
        }


    }
}
