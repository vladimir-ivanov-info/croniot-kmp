package com.croniot.client.domain.usecases

import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TasksRepository
import com.croniot.client.domain.repositories.TaskTypesRepository

class StartDeviceListenersUseCase(
    private val sensorDataRepository: SensorDataRepository,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) {

    suspend operator fun invoke(devices: List<Device>) {
        for (device in devices) {
            sensorDataRepository.listenToDeviceSensors(device)

            tasksRepository.listenTasks(device.uuid)
            tasksRepository.listenTaskStateInfos(device.uuid)

            for (taskType in device.taskTypes) {
                taskTypesRepository.add(device.uuid, taskType)
            }
        }
    }
}