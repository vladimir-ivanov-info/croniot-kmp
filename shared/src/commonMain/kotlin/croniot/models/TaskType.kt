package croniot.models

import croniot.models.dto.TaskTypeDto
import java.util.*
/*
data class TaskType(
    var id: Long = 0,
    var uid: Long = 0,
    var name: String = "",
    var description: String = "",
    var parameters: MutableSet<ParameterTask> = mutableSetOf(),
    var tasks: MutableSet<Task> = mutableSetOf(),
    var realTime: Boolean = false,
    @Transient
    var device: Device? = null,

    ) {*/

data class TaskType(
    var id: Long,
    var uid: Long,
    var name: String,
    var description: String,
    // var parameters: MutableSet<ParameterTask> = mutableSetOf(),
    // var tasks: MutableSet<Task> = mutableSetOf(),
    var parameters: List<ParameterTask> = emptyList(),
    var tasks: List<Task> = emptyList(),
    var realTime: Boolean,

    // @Transient
    var device: Device? = null,

    // var paramOrder: Int? = null

) {

    constructor() : this(0, 0, "", "", emptyList(), emptyList(), false, Device())
    constructor(
        uid: Long,
        name: String,
        description: String,
        // parameters: MutableSet<ParameterTask>,
        parameters: List<ParameterTask>,
        realTime: Boolean,
        device: Device,
    ) : this(0, uid, name, description, parameters, emptyList(), realTime, device)

    override fun hashCode(): Int {
        // return Objects.hash(id) // TODO or hash other relevant properties
        return id.hashCode()
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
    realTime = this.realTime,
)
