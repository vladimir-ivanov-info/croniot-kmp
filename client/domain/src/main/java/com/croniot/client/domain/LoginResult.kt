package com.croniot.client.domain

import com.croniot.client.domain.models.Account
import com.croniot.client.domain.models.auth.AuthTokens

data class LoginResult(
    val account: Account,
    val tokens: AuthTokens,
)