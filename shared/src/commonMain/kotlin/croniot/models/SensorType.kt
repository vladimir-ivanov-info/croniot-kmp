package croniot.models

import croniot.models.dto.SensorTypeDto

data class SensorType(
    var id: Long,
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterSensor>,
    @Transient
    var device: Device

    ) {

    constructor() : this(0, 0,"", "", mutableSetOf(), Device())
    constructor(uid: Long, name: String, description: String, parameters: MutableSet<ParameterSensor>, device: Device) : this(0, uid, name, description, parameters, device)

    constructor(id: Long, uid: Long) : this(id, uid, "", "", mutableSetOf(), Device())

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + device.hashCode()

        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as SensorType
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "Sensor type: $id"
    }
}

fun SensorType.toDto() = SensorTypeDto(
    uid = this.uid,
    name = this.name,
    description = this.description,
    parameters = this.parameters.map { it.toDto() }.toMutableSet()
)