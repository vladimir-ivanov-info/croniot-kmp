package com.croniot.client.core.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    var uuid: String,
    var nickname: String,
    var email: String,
    var devices: List<Device>,
)
