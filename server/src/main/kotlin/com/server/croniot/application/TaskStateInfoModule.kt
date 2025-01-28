package com.server.croniot.application

import com.croniot.server.db.daos.TaskStateInfoDao
import com.croniot.server.db.daos.TaskStateInfoDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class TaskStateInfoModule {
    @Binds
    @Singleton
    abstract fun bindTaskStateInfoDao(taskStateInfoDaoImpl: TaskStateInfoDaoImpl): TaskStateInfoDao
}