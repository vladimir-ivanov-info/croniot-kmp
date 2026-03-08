package com.server.croniot.application

import Global
import com.server.croniot.controllers.AccountController
import com.server.croniot.controllers.DeviceController
import com.server.croniot.controllers.SensorTypeController
import com.server.croniot.controllers.TaskController
import com.server.croniot.controllers.TaskTypeController
import com.server.croniot.data.db.daos.AccountDao
import com.server.croniot.data.db.daos.AccountJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceDao
import com.server.croniot.data.db.daos.DeviceJooqDaoImpl
import com.server.croniot.data.db.daos.DeviceTokenDao
import com.server.croniot.data.db.daos.DeviceTokenJooqDaoImpl
import com.server.croniot.data.db.daos.ParameterTaskDao
import com.server.croniot.data.db.daos.ParameterTaskDaoJooqImpl
import com.server.croniot.data.db.daos.SensorTypeDao
import com.server.croniot.data.db.daos.SensorTypeJooqDaoImpl
import com.server.croniot.data.db.daos.TaskDao
import com.server.croniot.data.db.daos.TaskDaoJooqImpl
import com.server.croniot.data.db.daos.TaskStateInfoDao
import com.server.croniot.data.db.daos.TaskStateInfoDaoJooqImpl
import com.server.croniot.data.db.daos.TaskTypeDao
import com.server.croniot.data.db.daos.TaskTypeDaoJooqImpl
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import com.server.croniot.data.repositories.TaskRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import com.server.croniot.http.SensorsDataController
import com.server.croniot.services.AccountService
import com.server.croniot.services.DeviceService
import com.server.croniot.services.SensorTypeService
import com.server.croniot.services.TaskService
import com.server.croniot.services.TaskTypeService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import javax.inject.Singleton
import javax.sql.DataSource

@Module
class AppModule {

    @Provides
    @Singleton
    fun provideDataSource(): DataSource {
        val secrets = Global.secrets
        val config = HikariConfig().apply {
            jdbcUrl = secrets.databaseUrl
            username = secrets.databaseUser
            password = secrets.databasePassword

            maximumPoolSize = 8
            minimumIdle = 2
            idleTimeout = 30_000
            maxLifetime = 30 * 60_000
            connectionTimeout = 10_000

            poolName = "croniot-hikari"
        }

        return HikariDataSource(config)
    }

    @Provides
    @Singleton
    fun provideDslContext(dataSource: DataSource): DSLContext {
        return DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    // --- Controllers ---

    @Provides
    @Singleton
    fun provideAccountController(accountService: AccountService): AccountController {
        return AccountController(accountService)
    }

    @Provides
    @Singleton
    fun provideDeviceController(deviceService: DeviceService): DeviceController {
        return DeviceController(deviceService)
    }

    @Provides
    @Singleton
    fun provideSensorTypeController(sensorTypeService: SensorTypeService): SensorTypeController {
        return SensorTypeController(sensorTypeService)
    }

    @Provides
    @Singleton
    fun provideTaskTypeController(taskTypeService: TaskTypeService): TaskTypeController {
        return TaskTypeController(taskTypeService)
    }

    @Provides
    @Singleton
    fun provideTaskController(taskService: TaskService, taskTypeService: TaskTypeService, deviceService: DeviceService, taskRepository: TaskRepository): TaskController {
        return TaskController(taskService, taskTypeService, deviceService, taskRepository)
    }

    @Provides
    @Singleton
    fun provideSensorsDataController(deviceService: DeviceService): SensorsDataController {
        return SensorsDataController(deviceService)
    }

    // --- DAOs ---

    @Provides @Singleton
    fun provideAccountDao(dsl: DSLContext): AccountDao =
        AccountJooqDaoImpl(dsl)

    @Provides @Singleton
    fun provideDeviceDao(dsl: DSLContext): DeviceDao =
        DeviceJooqDaoImpl(dsl)

    @Provides @Singleton
    fun provideDeviceTokenDao(dsl: DSLContext): DeviceTokenDao =
        DeviceTokenJooqDaoImpl(dsl)

    @Provides @Singleton
    fun provideSensorTypeDao(dsl: DSLContext): SensorTypeDao =
        SensorTypeJooqDaoImpl(dsl)

    @Provides @Singleton
    fun provideTaskTypeDao(dsl: DSLContext): TaskTypeDao =
        TaskTypeDaoJooqImpl(dsl)

    @Provides @Singleton
    fun provideTaskDao(dsl: DSLContext): TaskDao =
        TaskDaoJooqImpl(dsl)

    @Provides @Singleton
    fun provideParameterTaskDao(dsl: DSLContext): ParameterTaskDao =
        ParameterTaskDaoJooqImpl(dsl)

    @Provides @Singleton
    fun provideTaskStateInfoDao(dsl: DSLContext): TaskStateInfoDao =
        TaskStateInfoDaoJooqImpl(dsl)

    // --- Repositories ---

    @Provides
    @Singleton
    fun provideAccountRepository(accountDao: AccountDao, deviceDao: DeviceDao, sensorTypeDao: SensorTypeDao, taskTypeDao: TaskTypeDao): AccountRepository {
        return AccountRepository(accountDao, deviceDao, sensorTypeDao, taskTypeDao)
    }

    @Provides
    @Singleton
    fun provideDeviceRepository(deviceDao: DeviceDao): DeviceRepository {
        return DeviceRepository(deviceDao)
    }

    @Provides
    @Singleton
    fun provideDeviceTokenRepository(deviceTokenDao: DeviceTokenDao): DeviceTokenRepository {
        return DeviceTokenRepository(deviceTokenDao)
    }

    @Provides
    @Singleton
    fun provideSensorTypeRepository(sensorTypeDao: SensorTypeDao): SensorTypeRepository {
        return SensorTypeRepository(sensorTypeDao)
    }

    @Provides
    @Singleton
    fun provideTaskTypeRepository(taskTypeDao: TaskTypeDao, parameterTaskDao: ParameterTaskDao): TaskTypeRepository {
        return TaskTypeRepository(taskTypeDao, parameterTaskDao)
    }
}
