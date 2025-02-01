package croniot.models

import croniot.models.dto.TaskDto
import java.util.*

data class Task(
    var id: Long,
    var uid: Long,
    var parametersValues: MutableMap<ParameterTask, String>,
    @Transient
    var taskType: TaskType,
    @Transient
    var stateInfos: MutableSet<TaskStateInfo>,
) {

    constructor() : this(0, 0, mutableMapOf(), TaskType(), mutableSetOf())
    constructor(
        uid: Long,
        parametersValues: MutableMap<ParameterTask, String>,
        taskType: TaskType,
        stateInfos: MutableSet<TaskStateInfo>,
    ) :
        this(0, uid, parametersValues, taskType, stateInfos)

    override fun hashCode(): Int {
        return Objects.hash(id) // TODO or hash other relevant properties
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as Task
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "Task: $id"
    }
}

fun Task.toDto() = TaskDto(
    deviceUuid = this.taskType.device!!.uuid,
    taskTypeUid = this.taskType.uid,
    uid = this.uid,
    parametersValues = this.parametersValues.mapKeys { it.key.uid }.toMutableMap(),
    stateInfos = this.stateInfos.map { it.toDto() }.toMutableSet(), // ,
)
