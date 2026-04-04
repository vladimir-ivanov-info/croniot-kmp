package com.croniot.client.domain.models.auth

data class AuthSession(
    val email: String,
    val token: String,
)
