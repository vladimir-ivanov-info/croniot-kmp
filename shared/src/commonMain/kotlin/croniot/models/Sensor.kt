package croniot.models

import com.google.gson.annotations.Expose
import croniot.models.dto.DeviceDto
import croniot.models.dto.SensorDto
import java.util.*

data class Sensor(
    var id: Long = 0,
    var uid: Long = 0,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterSensor>,
    @Transient
    var device: Device

    ) {

    constructor() : this(0, 0,"", "", mutableSetOf(), Device())
    constructor(uid: Long, name: String, description: String, parameters: MutableSet<ParameterSensor>, device: Device) : this(0, uid, name, description, parameters, device)

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
        val (id1) = obj as Sensor
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "Sensor: $id"
    }
}

fun Sensor.toDto() = SensorDto(
    uid = this.uid,
    name = this.name,
    description = this.description,
    parameters = this.parameters.map { it.toDto() }.toMutableSet()
)