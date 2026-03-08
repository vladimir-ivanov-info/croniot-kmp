package com.croniot.client.domain.errors

sealed class RemoteError {
    data object Unreachable : RemoteError()
    data class Http(val code: Int) : RemoteError()
    data class ServerError(val message: String) : RemoteError()
    data object Unknown : RemoteError()
}
