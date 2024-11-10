package croniot.models

object TaskState {
    const val UNDEFINED = "UNDEFINED"
    const val CREATED = "CREATED"
    const val RECEIVED = "RECEIVED"
    const val STORED_LOCALLY_IN_DESTINATION = "STORED_LOCALLY_IN_DESTINATION"
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val ERROR = "ERROR"
    const val PAUSED = "PAUSED"
    const val ACCESSED_BY_DESTINATION = "ACCESSED_BY_DESTINATION"
}