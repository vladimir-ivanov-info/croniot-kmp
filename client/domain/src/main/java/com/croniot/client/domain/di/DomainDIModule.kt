package com.croniot.client.domain.di

import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TaskTypesRepositoryImpl
import com.croniot.client.domain.usecases.LogoutUseCase
import org.koin.dsl.module

val domainDiModule = module {

    single<TaskTypesRepository> { TaskTypesRepositoryImpl() }

    factory { LogoutUseCase(sessionRepository = get()) }

}