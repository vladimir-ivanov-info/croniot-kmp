package com.croniot.client.core.models.auth

data class AuthSession(
    val email: String,
    val token: String,
)
