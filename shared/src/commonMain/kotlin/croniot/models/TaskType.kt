package croniot.models

import croniot.models.dto.TaskTypeDto
import java.util.*

data class TaskType(
    var id: Long,
    var uid: Long,
    var name: String,
    var description: String,
    var parameters: MutableSet<ParameterTask>,
    var tasks: MutableSet<Task>,
    var realTime: Boolean,
    @Transient
    var device: Device

    ) {

    constructor(): this(0, 0, "", "", mutableSetOf(), mutableSetOf(), false, Device())
    constructor(uid: Long, name: String,
                description: String,
                parameters: MutableSet<ParameterTask>,
                realTime: Boolean,
                device: Device): this(0, uid, name, description, parameters, mutableSetOf(), realTime, device)

    override fun hashCode(): Int {
        return Objects.hash(id) //TODO or hash other relevant properties
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as TaskType
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "Task: $id"
    }
}

fun TaskType.toDto() = TaskTypeDto(
    uid = this.uid,
    name = this.name,
    description = this.description,
    parameters = this.parameters.map { it.toDto() }.toMutableSet(),
    realTime = this.realTime
)