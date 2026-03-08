package com.croniot.client.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText()

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args.toTypedArray())
    }

    @Composable
    fun asString(): String = asString(LocalContext.current)
}
