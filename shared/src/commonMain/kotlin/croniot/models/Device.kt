package croniot.models

import croniot.models.dto.DeviceDto

data class Device(
    var id: Long,
    var uuid: String,
    var name: String,
    var description: String,
    var iot: Boolean,
    var sensorTypes: MutableSet<SensorType>,
    var taskTypes: MutableSet<TaskType>,
    var account: Account, // Reference to the Account this Device belongs to
    var deviceToken: DeviceToken? = null, // Ensure this property exists
    //var deviceProperties: MutableMap<String, String> = mutableMapOf()

) {
    constructor() : this(0, "", "",  "",/*"",*/false, mutableSetOf(), mutableSetOf(), Account(), null/*, mutableMapOf()*/)

    constructor(uuid: String = "",
                name: String = "",
                description: String = "",
                iot: Boolean = false,
                sensorTypes: MutableSet<SensorType> = mutableSetOf(),
                taskTypes: MutableSet<TaskType> = mutableSetOf(),
                account: Account = Account(),
                //deviceToken: DeviceToken?,
                //deviceProperties: MutableMap<String, String>
    ) : this(0, uuid, name, description, iot, sensorTypes, taskTypes, account/*, deviceToken, deviceProperties*/)


    constructor(id: Long, uuid: String) : this(id, uuid, "", "", false, mutableSetOf(), mutableSetOf(), Account(), null)


    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + account.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as Device
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "Device: $id"
    }
}

fun Device.toDto() = DeviceDto(
    uuid = this.uuid,
    name = this.name,
    description = this.description,
    sensors = this.sensorTypes.map { it.toDto() }.toMutableSet(),
    tasks = this.taskTypes.map { it.toDto() }.toMutableSet(),
    lastOnlineMillis = 0, //TODO implement a column in database. Another column for seconds after which the device should be considered offline.
)
