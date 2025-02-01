package com.server.croniot.application

import com.server.croniot.data.db.controllers.ControllerDb
import com.server.croniot.data.db.daos.AccountDao
import com.server.croniot.data.db.daos.AccountDaoImpl
import com.server.croniot.data.db.daos.DeviceDao
import com.server.croniot.data.db.daos.DeviceDaoImpl
import com.server.croniot.data.db.daos.DeviceTokenDao
import com.server.croniot.data.db.daos.DeviceTokenDaoImpl
import com.server.croniot.data.db.daos.ParameterTaskDao
import com.server.croniot.data.db.daos.ParameterTaskDaoImpl
import com.server.croniot.data.db.daos.SensorTypeDao
import com.server.croniot.data.db.daos.SensorTypeDaoImpl
import com.server.croniot.data.db.daos.TaskStateInfoDao
import com.server.croniot.data.db.daos.TaskStateInfoDaoImpl
import com.server.croniot.data.db.daos.TaskTypeDao
import com.server.croniot.data.db.daos.TaskTypeDaoImpl
import com.server.croniot.controllers.AccountController
import com.server.croniot.controllers.DeviceController
import com.server.croniot.controllers.DeviceTokenController
import com.server.croniot.controllers.SensorTypeController
import com.server.croniot.controllers.TaskController
import com.server.croniot.controllers.TaskTypeController
import com.server.croniot.data.repositories.AccountRepository
import com.server.croniot.data.repositories.DeviceRepository
import com.server.croniot.data.repositories.DeviceTokenRepository
import com.server.croniot.data.repositories.SensorTypeRepository
import com.server.croniot.data.repositories.TaskTypeRepository
import com.server.croniot.http.SensorsDataController
import com.server.croniot.services.AccountService
import com.server.croniot.services.DeviceService
import com.server.croniot.services.DeviceTokenService
import com.server.croniot.services.SensorTypeService
import com.server.croniot.services.TaskService
import com.server.croniot.services.TaskTypeService
import dagger.Module
import dagger.Provides
import com.server.croniot.data.db.daos.TaskDao
import com.server.croniot.data.db.daos.TaskDaoImpl
import org.hibernate.SessionFactory
import javax.inject.Singleton

@Module
class AppModule {

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
    fun provideDeviceTokenController(deviceTokenService: DeviceTokenService): DeviceTokenController {
        return DeviceTokenController(deviceTokenService)
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
    fun provideTaskController(taskService: TaskService, taskTypeService: TaskTypeService, deviceService: DeviceService): TaskController {
        return TaskController(taskService, taskTypeService, deviceService)
    }

    @Provides
    @Singleton
    fun provideSensorsDataController(deviceService: DeviceService) : SensorsDataController {
        return SensorsDataController(deviceService)
    }

    @Provides
    @Singleton
    fun provideAccountDao(sessionFactory: SessionFactory): AccountDao {
        return AccountDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideDeviceDao(sessionFactory: SessionFactory): DeviceDao {
        return DeviceDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideDeviceTokenDao(sessionFactory: SessionFactory): DeviceTokenDao {
        return DeviceTokenDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideSensorTypeDao(sessionFactory: SessionFactory): SensorTypeDao {
        return SensorTypeDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideTaskTypeDao(sessionFactory: SessionFactory): TaskTypeDao {
        return TaskTypeDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideTaskDao(sessionFactory: SessionFactory): TaskDao {
        return TaskDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideParameterTaskDao(sessionFactory: SessionFactory): ParameterTaskDao {
        return ParameterTaskDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideTaskStateInfoDao(sessionFactory: SessionFactory): TaskStateInfoDao {
        return TaskStateInfoDaoImpl(sessionFactory)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(accountDao: AccountDao): AccountRepository {
        return AccountRepository(accountDao)
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

    @Provides
    @Singleton
    fun provideSessionFactory(): SessionFactory {
        return ControllerDb.sessionFactory
    }


}