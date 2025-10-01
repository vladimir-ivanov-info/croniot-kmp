package com.croniot.client.core

import java.util.*

object Global {

    val appName = "croniot"

    val IS_EVREYTHING_MOCKED = false

    // TODO move to a separate util class
    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        val uniqueString = uuid.toString().substring(0, length)
        return uniqueString
    }
}
