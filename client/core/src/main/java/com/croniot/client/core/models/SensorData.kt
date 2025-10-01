package com.croniot.client.core.models

import com.croniot.client.core.demo.DemoConstants
import java.time.ZonedDateTime
import kotlin.random.Random

data class SensorData(
    var deviceUuid: String,
    var sensorTypeUid: Long,
    var value: String,
    var timeStamp: ZonedDateTime,
) {

    companion object { //TODO maybe delegate the fake data generation to a different class like FakeDataGenerator.generateFakeSensorData()
        fun generateRandomIntData(deviceUuid: String, sensorTypeUid: Long, minValue: Int, maxValue: Int) : SensorData {
            val sensorRandomValue = Random.nextInt(
                from = minValue,
                until = maxValue
            )

            return SensorData(
                deviceUuid = deviceUuid,
                sensorTypeUid = sensorTypeUid,
                value = sensorRandomValue.toString(),
                timeStamp = ZonedDateTime.now()
            )
        }
    }

}


