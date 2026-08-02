package com.croniot.client.presentation

import com.croniot.client.domain.errors.RemoteError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RemoteErrorUiTextTest {

    @Test
    fun `WHEN the error is Unreachable THEN toUiText maps it to the no-connection resource`() {
        val result = RemoteError.Unreachable.toUiText()

        assertEquals(UiText.Resource(R.string.error_no_connection), result)
    }

    @Test
    fun `WHEN the error is Http THEN toUiText maps it to the server error resource with the status code as an argument`() {
        val result = RemoteError.Http(code = 503).toUiText()

        assertTrue(result is UiText.Resource)
        assertEquals(R.string.error_server, (result as UiText.Resource).id)
        assertEquals(listOf(503), result.args)
    }

    @Test
    fun `WHEN the error is ServerError THEN toUiText maps it to a dynamic text carrying the message`() {
        val result = RemoteError.ServerError(message = "custom failure").toUiText()

        assertEquals(UiText.Dynamic("custom failure"), result)
    }

    @Test
    fun `WHEN the error is Unknown THEN toUiText maps it to the generic error resource`() {
        val result = RemoteError.Unknown.toUiText()

        assertEquals(UiText.Resource(R.string.error_unknown), result)
    }
}
