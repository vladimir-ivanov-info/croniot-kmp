package croniot.models

data class SensorInfoDb(val uuid: String, val id: String, val name: String = "", val type: String = "", val unit: String = "", val description: String = "")
