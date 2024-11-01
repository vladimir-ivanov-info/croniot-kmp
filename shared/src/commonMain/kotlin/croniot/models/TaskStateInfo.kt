package croniot.models

import croniot.models.dto.TaskStateInfoDto
import java.time.ZonedDateTime
import java.util.*

data class TaskStateInfo(
    var id: Long = 0,

    var dateTime: ZonedDateTime,
    var state: TaskState,
    var progress: Double,
    var errorMessage: String,
    @Transient
    var task: Task,
) {

    constructor(): this(0, ZonedDateTime.now(), TaskState.UNDEFINED, 0.0, "", Task())
    constructor(dateTime: ZonedDateTime, state: TaskState, progress: Double, errorMessage: String, task: Task): this(0, dateTime, state, progress, errorMessage, task)

    override fun hashCode(): Int {
        return Objects.hash(id) //TODO or hash other relevant properties
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (obj == null || javaClass != obj.javaClass) return false
        val (id1) = obj as TaskStateInfo
        return id == id1 // or compare other relevant properties
    }

    override fun toString(): String {
        return "TaskStateInfo: $id"
    }
}

fun TaskStateInfo.toDto() = TaskStateInfoDto(
    deviceUuid =  this.task.taskType.device!!.uuid,
    taskTypeUid = this.task.taskType.uid,
    taskUid = this.task.uid,
    dateTime = this.dateTime,
    state = this.state,
    progress = this.progress,
    errorMessage = this.errorMessage

)