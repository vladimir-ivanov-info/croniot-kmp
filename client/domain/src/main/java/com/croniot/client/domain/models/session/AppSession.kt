package com.croniot.client.domain.models.session

import com.croniot.client.domain.models.Account

sealed interface AppSession {
    data object None : AppSession
    data class Server(val account: Account) : AppSession
    data object BleOnly : AppSession
}
