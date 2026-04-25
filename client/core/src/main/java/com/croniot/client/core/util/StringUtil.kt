package com.croniot.client.core.util

import java.util.UUID

object StringUtil {
    fun generateRandomString(length: Int = 3): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun generateUniqueString(length: Int): String {
        val uuid = UUID.randomUUID()
        val uniqueString = uuid.toString().substring(0, length)
        return uniqueString
    }
}

fun getRelativeTimeText(now: Long, last: Long?): String {
    if (last == null || last <= 0L) return "no recent signal"
    val diff = (now - last).coerceAtLeast(0L)
    val seconds = diff / 1000
    return when {
        seconds < 5 -> "in real time"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60} min ago"
        else -> "${seconds / 3600} h ago"
    }
}
