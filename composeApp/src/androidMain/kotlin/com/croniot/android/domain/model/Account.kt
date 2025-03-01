package com.croniot.android.domain.model

data class Account(
    var uuid: String,
    var nickname: String,
    var email: String,
    var devices: MutableSet<Device>,
)
