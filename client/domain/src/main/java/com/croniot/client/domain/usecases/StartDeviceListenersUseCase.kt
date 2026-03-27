package com.croniot.client.domain.usecases

import Outcome
import android.util.Log
import com.croniot.client.core.models.ConnectionError
import com.croniot.client.core.models.Device
import com.croniot.client.domain.repositories.SensorDataRepository
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.repositories.TasksRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

class StartDeviceListenersUseCase(
    private val sensorDataRepository: SensorDataRepository,
    private val tasksRepository: TasksRepository,
    private val taskTypesRepository: TaskTypesRepository,
) {

    suspend operator fun invoke(devices: List<Device>): Outcome<Unit, List<ConnectionError>> = coroutineScope {

        val filteredDevices = devices.filter { !it.uuid.startsWith("android") } //TODO patch

        val results = filteredDevices.map { device ->
            async {
                try {
                    val sensorResult = sensorDataRepository.listenToDeviceSensors(device)
                    tasksRepository.listenTasks(device.uuid)
                    tasksRepository.listenTaskStateInfos(device.uuid)
                    for (taskType in device.taskTypes) {
                        taskTypesRepository.add(device.uuid, taskType)
                    }
                    sensorResult
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Listeners", "Failed for device ${device.uuid}", e)
                    Outcome.Err(ConnectionError.Unknown)
                }
            }
        }.awaitAll()

        val errors = results.filterIsInstance<Outcome.Err<ConnectionError>>().map { it.error }

        if (errors.isEmpty()) Outcome.Ok(Unit) else Outcome.Err(errors)
    }

}
