package croniot.models

enum class TaskState {
    UNDEFINED,
    CREATED,
    RECEIVED,
    STORED_LOCALLY_IN_DESTINATION,
    RUNNING,
    COMPLETED,
    ERROR,
    PAUSED,
    ACCESSED_BY_DESTINATION;

    companion object {
        fun fromString(value: String): TaskState =
            entries.find { it.name == value } ?: UNDEFINED
    }
}