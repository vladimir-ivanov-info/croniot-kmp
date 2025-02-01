package com.croniot.android.core.util

import java.time.Duration
import java.time.ZonedDateTime

object DateTimeUtil {

    fun formatRelativeTime(dateTime: ZonedDateTime): String {
        val now = ZonedDateTime.now()
        val duration = Duration.between(now, dateTime)
        val isPast = duration.isNegative

        val absDuration = if (isPast) duration.abs() else duration

        val years = absDuration.toDays() / 365
        val months = (absDuration.toDays() % 365) / 30
        val weeks = (absDuration.toDays() % 30) / 7
        val days = absDuration.toDays() % 7
        val hours = absDuration.toHours() % 24
        val minutes = absDuration.toMinutes() % 60
        val seconds = absDuration.seconds % 60

        val timeString = buildString {
            if (years > 0) {
                append("${years}y ")
                append("${months}m ")
            } else if (months > 0) {
                append("${months}m ")
                append("${weeks}w ")
            } else if (weeks > 0) {
                append("${weeks}w ")
                append("${days}d ")
            } else if (days > 0) {
                append("${days}d ")
                append("${hours}h ")
            } else if (hours > 0) {
                append("${hours}h ")
                append("${minutes}m ")
            } else if (minutes > 0) {
                append("${minutes}m ")
                append("${seconds}s ")
            } else {
                append("${seconds}s ")
            }
        }.trim()

        return if (isPast) "$timeString ago" else "in $timeString"
    }
}
