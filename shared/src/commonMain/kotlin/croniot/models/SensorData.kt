package croniot.models

import croniot.models.dto.SensorDataDto
import java.time.ZonedDateTime

data class SensorData(
    var id: Long = 0,
    var device: Device,
    var sensorType : SensorType,
    var value: String,
    var dateTime: ZonedDateTime) {

    constructor(): this(0, Device(), SensorType(), "", ZonedDateTime.now())
    constructor(device: Device, sensorType: SensorType, value: String, dateTime: ZonedDateTime)
            : this(0, device, sensorType, value, dateTime)


    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + device.hashCode()
        result = 31 * result + sensorType.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + dateTime.hashCode()

        return result
    }
    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as SensorData
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "SensorData: ${device.uuid} ${sensorType.uid} $id $value"
    }
}

fun SensorData.toDto() = SensorDataDto(
    deviceUuid = device.uuid,
    sensorTypeUid = sensorType.uid,
    value = value,
    timestamp = dateTime
)