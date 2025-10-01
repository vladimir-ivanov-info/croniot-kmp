package com.croniot.client.core.models.auth

data class AuthSession(
    val email: String,
    val token: String
)

sealed interface AuthError {
    data object Network : AuthError
    data object InvalidCredentials : AuthError
    data object AccountMissing : AuthError
    data object TokenMissing : AuthError
    data object DeviceMissing : AuthError
    data class Server(val message: String?) : AuthError
    data object Unknown : AuthError
}

sealed class Outcome<out T, out E> {
    data class Ok<T>(val value: T) : Outcome<T, Nothing>()
    data class Err<E>(val error: E) : Outcome<Nothing, E>()
}