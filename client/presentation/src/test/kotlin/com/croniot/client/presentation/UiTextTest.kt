package com.croniot.client.presentation

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UiTextTest {

    @Test
    fun `WHEN the UiText is Dynamic THEN asString returns its value regardless of context`() {
        val context: Context = mockk()

        val result = UiText.Dynamic("hello world").asString(context)

        assertEquals("hello world", result)
    }

    @Test
    fun `WHEN the UiText is a Resource THEN asString delegates to context getString with the given id`() {
        val context: Context = mockk()
        every { context.getString(42, *arrayOf<Any>()) } returns "resolved string"

        val result = UiText.Resource(id = 42).asString(context)

        assertEquals("resolved string", result)
    }

    @Test
    fun `WHEN the Resource has args THEN asString passes them to context getString`() {
        val context: Context = mockk()
        every { context.getString(42, "arg1", 2) } returns "formatted arg1 2"

        val result = UiText.Resource(id = 42, args = listOf("arg1", 2)).asString(context)

        assertEquals("formatted arg1 2", result)
    }
}
