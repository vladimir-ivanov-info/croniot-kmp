package com.croniot.android.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppError(
    val title: String,
    val message: String,
) {
    fun encode(): String = Json.encodeToString(this)

    companion object {
        fun decode(json: String?): AppError? =
            json?.let { runCatching { Json.decodeFromString<AppError>(it) }.getOrNull() }
    }
}