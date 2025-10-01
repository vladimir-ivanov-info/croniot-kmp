package com.croniot.client.features.sensors.data.datasource.remote

import com.croniot.client.core.models.SensorData
import com.croniot.client.data.sensors.datasource.RemoteSensorDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import kotlin.random.Random

class RemoteSensorDataSourceImplDemo() : RemoteSensorDataSource {

    override suspend fun listenDeviceSensors(
        deviceUuid: String,
        onNewSensorData: (sensorData: SensorData) -> Unit,
    ) {
        // TODO randomly generate values

        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val sensorRandomValue = Random.nextInt(from = -90, until = -30)

                val sensorData = SensorData(
                    deviceUuid = deviceUuid, // usa el que recibes
                    sensorTypeUid = 1L,
                    value = sensorRandomValue.toString(), // random sencillo
                    timeStamp = ZonedDateTime.now(),
                )

                onNewSensorData(sensorData)

                delay(1000)
            }
        }
    }

    override suspend fun stopListening(deviceUuid: String?) {
        // TODO
    }
}
