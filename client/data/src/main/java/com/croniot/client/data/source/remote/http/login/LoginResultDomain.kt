package com.croniot.client.data.source.remote.http.login

import com.croniot.client.core.models.Account

data class LoginResultDomain(
    val account: Account,
    val token: String
)