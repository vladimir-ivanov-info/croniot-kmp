package com.croniot.client.domain.errors

sealed class TaskError {
    data class Remote(val error: RemoteError) : TaskError()
}
