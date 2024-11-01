package croniot.models

import croniot.models.dto.ParameterSensorDto
import java.util.*

class ParameterSensor(
    id: Long,
    uid: Long,
    name: String,
    type: String,
    unit: String,
    description: String,
    constraints: MutableMap<String, String>,
    var sensorTypeId: Long? = null, // New property
    //@Transient
    var sensorType: SensorType

) : Parameter(id = id, uid = uid, name = name, type = type, unit = unit, description = description, constraints = constraints) {

    constructor(): this(0, 0,"", "", "", "", mutableMapOf(), null, SensorType())
    constructor(uid: Long, name: String, type: String, unit: String, description: String, constraints: MutableMap<String, String>, sensorType: SensorType) :
            this(0, uid, name, type, unit, description, constraints,  sensorType.id, sensorType)

    override fun hashCode(): Int {
        //return Objects.hash(id) // or hash other relevant properties
        return Objects.hash(uid) // or hash other relevant properties
    }

   /* override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as Device
        return id == id1 // or compare other relevant properties
    }*/

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val other = obj as ParameterSensor
        //return id == other.id // or compare other relevant properties
        return uid == other.uid // or compare other relevant properties
    }

    override fun toString(): String {
        return "ParameterSensor: $id"
    }

}

fun ParameterSensor.toDto() = ParameterSensorDto(
    uid = this.uid,
    name = this.name,
    type = this.type,
    unit = this.unit,
    description = this.description,
    constraints = this.constraints
)