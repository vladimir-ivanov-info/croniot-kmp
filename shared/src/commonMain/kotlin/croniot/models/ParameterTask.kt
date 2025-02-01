package croniot.models

import croniot.models.dto.ParameterTaskDto
import java.util.*

class ParameterTask(
    id: Long,
    uid: Long,
    name: String,
    type: String,
    unit: String,
    description: String,
    constraints: MutableMap<String, String> = mutableMapOf(),
    //  @Transient
    var taskTypeId: Long? = null, // New property
    //  @Transient
    var taskType: TaskType? = null,

) : Parameter(id = id, uid = uid, name = name, type = type, unit = unit, description = description, constraints = constraints) {

    constructor() : this(0, 0, "", "", "", "", mutableMapOf(), null, null)

    constructor(uid: Long, name: String, type: String, unit: String, description: String, constraints: MutableMap<String, String>, taskType: TaskType) :
        this(0, uid, name, type, unit, description, constraints, taskType.id, taskType)

    override fun hashCode(): Int {
        // return Objects.hash(id) // or hash other relevant properties
        return Objects.hash(uid) // or hash other relevant properties
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val other = obj as ParameterTask
        // return id == other.id // or compare other relevant properties
        return uid == other.uid // or compare other relevant properties
    }

    override fun toString(): String {
        return "ParameterTask: $id"
    }
}

fun ParameterTask.toDto() = ParameterTaskDto(
    uid = this.uid,
    name = this.name,
    type = this.type,
    unit = this.unit,
    description = this.description,
    constraints = this.constraints,
)
