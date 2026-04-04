package com.croniot.client.domain.models.auth

sealed interface AuthError {
    data object Network : AuthError
    data object NetworkTiemout : AuthError
    data object InvalidCredentials : AuthError
    data object AccountMissing : AuthError
    data object TokenMissing : AuthError
    data object DeviceMissing : AuthError
    data class Server(val message: String?) : AuthError
    data object Unknown : AuthError
}
