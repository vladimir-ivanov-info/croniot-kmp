package com.croniot.client.data.source.local

data class LocalConfiguration( //TODO
    private val selectedAccountEmail: String,
    private val selectedDeviceUuid: String,
    private val selectedDeviceToken: String,
    private val currentScreen: String,
)
