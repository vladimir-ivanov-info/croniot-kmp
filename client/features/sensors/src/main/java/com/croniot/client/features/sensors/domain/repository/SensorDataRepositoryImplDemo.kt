//package com.croniot.client.features.sensors.domain.repository
//
//import com.croniot.client.core.models.Device
//import com.croniot.client.core.models.SensorData
//import com.croniot.client.domain.repositories.SensorDataRepository
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.flowOf
//import java.time.ZonedDateTime
//
//class SensorDataRepositoryImplDemo : SensorDataRepository {
//
//
//
//    override suspend fun listenToDeviceSensors(device: Device) {
//        //TODO("Not yet implemented")
//
//    }
//
//    override fun observeSensorData(deviceUuid: String, sensorTypeUid: Long): StateFlow<SensorData> {
//        //TODO("Not yet implemented")
//
//        return MutableStateFlow(
//            SensorData(
//                deviceUuid = deviceUuid,
//                sensorTypeUid = sensorTypeUid,
//                value = "42",
//                timeStamp = ZonedDateTime.now()
//            )
//        )
//    }
//
//    override suspend fun getLatestSensorData(
//        deviceUuid: String,
//        sensorTypeUid: Long,
//        elements: Int
//    ): List<SensorData> {
//        //TODO("Not yet implemented")
//
//        return emptyList()
//    }
//
//   /* override fun observeSensorDataInsertions(deviceUuid: String): Flow<Long> {
//        //TODO("Not yet implemented")
//        return flowOf()
//    }*/
//
//}