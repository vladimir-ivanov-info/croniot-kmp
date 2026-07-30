package com.croniot.android.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppErrorTest {

    @Test
    fun `encode then decode roundtrips to an equal AppError`() {
        val error = AppError(title = "Connection error", message = "Could not reach the server")

        val json = error.encode()
        val decoded = AppError.decode(json)

        assertEquals(error, decoded)
    }

    @Test
    fun `decode with null input returns null`() {
        assertNull(AppError.decode(null))
    }

    @Test
    fun `decode with invalid json returns null instead of throwing`() {
        assertNull(AppError.decode("not valid json {"))
    }

    @Test
    fun `decode with json missing required fields returns null`() {
        assertNull(AppError.decode("""{"title":"Only title"}"""))
    }

    @Test
    fun `encode produces valid json containing title and message`() {
        val error = AppError(title = "Title", message = "Message")

        val json = error.encode()

        assertEquals(true, json.contains("Title"))
        assertEquals(true, json.contains("Message"))
    }
}
