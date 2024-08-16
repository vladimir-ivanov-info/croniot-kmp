package croniot.models

enum class TaskState {
    UNDEFINED,
    CREATED,
    ACCESSED_BY_DESTINATION,
    RECEIVED,
    STORED_LOCALLY_IN_DESTINATION,
    RUNNING,
    //FINISHED,
    COMPLETED,
    ERROR,
    PAUSED,
}