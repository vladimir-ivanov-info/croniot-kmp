package com.server.croniot.application

import com.croniot.server.db.daos.TaskTypeDao
import com.croniot.server.db.daos.TaskTypeDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class TaskTypeModule {
    @Binds
    @Singleton
    abstract fun bindTaskTypeDao(taskTypeDaoImpl: TaskTypeDaoImpl): TaskTypeDao
}