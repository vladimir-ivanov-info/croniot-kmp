package com.server.croniot.application

import com.croniot.server.db.daos.ParameterTaskDao
import com.croniot.server.db.daos.ParameterTaskDaoImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class ParameterTaskModule {
    @Binds
    @Singleton
    abstract fun bindParameterTaskDao(parameterTaskDaoImpl: ParameterTaskDaoImpl): ParameterTaskDao
}