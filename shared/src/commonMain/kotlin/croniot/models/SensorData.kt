package croniot.models

data class SensorData(val clientUuid: String, val sensorId : String, val value: String, val dateTime: String) {
}