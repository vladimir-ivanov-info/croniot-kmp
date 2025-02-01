package com.croniot.android.core.util

object StringUtil {
    fun generateRandomString(length: Int = 3): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
