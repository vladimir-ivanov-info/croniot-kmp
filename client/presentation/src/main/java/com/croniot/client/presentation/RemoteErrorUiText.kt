package com.croniot.client.presentation

import com.croniot.client.domain.errors.RemoteError

fun RemoteError.toUiText(): UiText = when (this) {
    is RemoteError.Unreachable -> UiText.Resource(R.string.error_no_connection)
    is RemoteError.Http -> UiText.Resource(R.string.error_server, listOf(code))
    is RemoteError.ServerError -> UiText.Dynamic(message)
    is RemoteError.Unknown -> UiText.Resource(R.string.error_unknown)
}
