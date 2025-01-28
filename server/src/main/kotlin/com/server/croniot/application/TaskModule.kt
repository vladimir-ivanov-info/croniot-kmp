package com.server.croniot.application

import dagger.Binds
import dagger.Module
import db.daos.TaskDao
import db.daos.TaskDaoImpl
import javax.inject.Singleton

@Module
abstract class TaskModule {
    @Binds
    @Singleton
    abstract fun bindTaskDao(taskDaoImpl: TaskDaoImpl): TaskDao
}