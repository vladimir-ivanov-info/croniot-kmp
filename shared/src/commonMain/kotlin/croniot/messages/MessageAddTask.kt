package croniot.messages

data class MessageAddTask(val deviceUuid: String, val taskUid: String, val parametersValues: MutableMap<Long, String>)

//TODO each task can have a protocol. For example, for real time low latency data we can use MQTT and for async tasks we can use HTTP