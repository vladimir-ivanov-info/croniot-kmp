package com.croniot.android.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppErrorTest {

    @Test
    fun `WHEN encode is followed by decode THEN it roundtrips to an equal AppError`() {
        val error = AppError(title = "Connection error", message = "Could not reach the server")

        val json = error.encode()
        val decoded = AppError.decode(json)

        assertEquals(error, decoded)
    }

    @Test
    fun `WHEN decode is called with null input THEN it returns null`() {
        assertNull(AppError.decode(null))
    }

    @Test
    fun `WHEN decode is called with invalid json THEN it returns null instead of throwing`() {
        assertNull(AppError.decode("not valid json {"))
    }

    @Test
    fun `WHEN decode is called with json missing required fields THEN it returns null`() {
        assertNull(AppError.decode("""{"title":"Only title"}"""))
    }

    @Test
    fun `WHEN encode is called THEN it produces valid json containing the title and message`() {
        val error = AppError(title = "Title", message = "Message")

        val json = error.encode()

        assertEquals(true, json.contains("Title"))
        assertEquals(true, json.contains("Message"))
    }
}
