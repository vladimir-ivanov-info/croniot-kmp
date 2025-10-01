package com.croniot.client.features.sensors.data.datasource.local

import com.croniot.client.core.demo.DemoConstants
import com.croniot.client.core.models.SensorData
import com.croniot.client.data.sensors.datasource.LocalSensorDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.ZonedDateTime

class LocalSensorDataSourceImplDemo : LocalSensorDataSource {

    override suspend fun save(sensorData: SensorData) {
        // TODO
    }

    override suspend fun getLatest(
        deviceUuid: String,
        sensorTypeUid: Long,
        limit: Int,
    ): List<SensorData> {
        // TODO
        return emptyList()
    }

    override /*suspend*/ fun observeSensorData(
        deviceUuid: String,
        sensorTypeUid: Long,
    ): /*State*/Flow<SensorData> {
        /*CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val sensorRandomValue = Random.nextInt(from = -90, until = -30)

                val sensorData = SensorData(
                    deviceUuid = deviceUuid,   // usa el que recibes
                    sensorTypeUid = 1L,
                    value = sensorRandomValue.toString(), // random sencillo
                    timeStamp = ZonedDateTime.now()
                )

                //onNewSensorData(sensorData)

                delay(1000)
            }
        }

        return MutableStateFlow(
            SensorData(
                deviceUuid = deviceUuid,
                sensorTypeUid = sensorTypeUid,
                value = "-51",
                timeStamp = ZonedDateTime.now()
            )
        )*/
        return getFakeSensorFlow(deviceUuid = deviceUuid, sensorTypeUid = sensorTypeUid)

        /*return flow {
            while (true) {
                val sensorRandomValue = Random.nextInt(from = -65, until = -50)
                emit(
                    SensorData(
                        deviceUuid = deviceUuid,
                        sensorTypeUid = sensorTypeUid,
                        value = sensorRandomValue.toString(),
                        timeStamp = ZonedDateTime.now()
                    )
                )
                delay(1000) // suspende, no bloquea
            }
        }*/
    }

    private fun getFakeSensorFlow(deviceUuid: String, sensorTypeUid: Long): Flow<SensorData> {
        return flow {
            while (true) {
                val sensorData = when (sensorTypeUid) {
                    DemoConstants.SENSOR_WIFI_LEVEL_ID -> {
                        /*val sensorRandomValue = Random.nextInt(
                            from = DemoConstants.SENSOR_WIFI_LEVEL_MIN_VALUE,
                            until = DemoConstants.SENSOR_WIFI_LEVEL_MAX_VALUE
                        )
                        SensorData(
                            deviceUuid = deviceUuid,
                            sensorTypeUid = sensorTypeUid,
                            value = sensorRandomValue.toString(),
                            timeStamp = ZonedDateTime.now()
                        )*/

                        SensorData.generateRandomIntData(
                            deviceUuid = deviceUuid,
                            sensorTypeUid = sensorTypeUid,
                            minValue = DemoConstants.SENSOR_WIFI_LEVEL_MIN_VALUE,
                            maxValue = DemoConstants.SENSOR_WIFI_LEVEL_MAX_VALUE,
                        )
                    }
                    else -> SensorData( // TODO
                        deviceUuid = deviceUuid,
                        sensorTypeUid = sensorTypeUid,
                        value = "123",
                        timeStamp = ZonedDateTime.now(),
                    )
                }
                emit(sensorData)
                delay(1000)
            }
        }
    }
}
