package com.croniot.client.core.models

data class Account(
    var uuid: String,
    var nickname: String,
    var email: String,
    var devices: List<Device>,
)
