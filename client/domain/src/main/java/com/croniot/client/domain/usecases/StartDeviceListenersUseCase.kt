package com.croniot.client.domain.usecases

import android.util.Log
import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class StartDeviceListenersUseCase(
    private val sensorDataRepository: SensorDataRepository,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) {

    suspend operator fun invoke(devices: List<Device>) = coroutineScope {

        val filteredDevices = devices.filter { !it.uuid.startsWith("android") } //TODO patch

        for (device in filteredDevices) {
            launch {
                try {
                    sensorDataRepository.listenToDeviceSensors(device)
                    tasksRepository.listenTasks(device.uuid)
                    tasksRepository.listenTaskStateInfos(device.uuid)
                    for (taskType in device.taskTypes) {
                        taskTypesRepository.add(device.uuid, taskType)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Listeners", "Failed for device ${device.uuid}", e)
                }
            }
        }
    }

}
