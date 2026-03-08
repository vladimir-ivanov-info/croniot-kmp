package com.croniot.client.domain.usecases

import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TasksRepository

class StopDeviceListenersUseCase(
    private val sensorDataRepository: SensorDataRepository,
    private val tasksRepository: TasksRepository,
) {

    suspend operator fun invoke() {
        sensorDataRepository.stopAllListeners()
        tasksRepository.stopAllListeners()
    }
}