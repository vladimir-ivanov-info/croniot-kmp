package com.croniot.android.features.device.features.sensors.data.processor

import ZonedDateTimeAdapter
import com.croniot.android.features.device.features.sensors.presentation.ViewModelSensors
import com.croniot.android.features.deviceslist.DevicesListViewModel
import com.google.gson.GsonBuilder
import croniot.models.MqttDataProcessor
import croniot.models.dto.SensorDataDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.ZonedDateTime

class MqttProcessorSensorData(val deviceuid: String, val sensorUuid: String, val onNewData : (String) -> Unit) : MqttDataProcessor, KoinComponent {

   // private val viewModelSensors: ViewModelSensors = get()
  //  private val devicesListViewModel: DevicesListViewModel = get()

    val gsonZonedDateTime = GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeAdapter())
        .setPrettyPrinting()
        .create()

    override fun getTopic(): String {
        TODO("Not yet implemented")
    }

    override fun process(data: Any) {
        //CoroutineScope(Dispatchers.IO).launch {
        //GlobalScope.launch {
            val message = data as String

            val sensorDataDto = gsonZonedDateTime.fromJson(message, SensorDataDto::class.java)

            onNewData(sensorDataDto.value)

            //viewModelSensors.updateSensorData(clientUuid, sensorUuid, sensorDataDto.value)
            //TODO devicesListViewModel.updateDeviceOnlineStatus(clientUuid)
        //}
    }
}