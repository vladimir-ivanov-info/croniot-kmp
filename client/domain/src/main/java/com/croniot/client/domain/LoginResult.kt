package com.croniot.client.domain

import com.croniot.client.core.models.Account

data class LoginResult(
    val account: Account,
    val token: String,
)
