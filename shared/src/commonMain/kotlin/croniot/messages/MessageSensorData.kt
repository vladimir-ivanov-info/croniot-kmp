package croniot.messages

data class MessageSensorData(val messageName: String = "sensor_data",
                             val id: String,
                             val value: String)