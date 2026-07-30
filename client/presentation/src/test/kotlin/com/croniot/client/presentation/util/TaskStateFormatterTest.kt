package com.croniot.client.presentation.util

import com.croniot.client.domain.models.TaskStateInfo
import croniot.models.TaskState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TaskStateFormatterTest {

    private val fixedTime = ZonedDateTime.parse("2026-01-01T14:05:00Z")

    private fun info(state: TaskState, progress: Double = 0.0, errorMessage: String = "") = TaskStateInfo(
        dateTime = fixedTime,
        state = state.name,
        progress = progress,
        errorMessage = errorMessage,
    )

    @Test
    fun `formatStateInfo returns empty string for null info`() {
        assertEquals("", formatStateInfo(null))
    }

    @Test
    fun `formatStateInfo for RUNNING includes the label, progress percentage and time`() {
        val result = formatStateInfo(info(TaskState.RUNNING, progress = 42.0))

        assertEquals("Running • 42% · 14:05", result)
    }

    @Test
    fun `formatStateInfo for ERROR includes the label, truncated error message and time`() {
        val longMessage = "x".repeat(80)

        val result = formatStateInfo(info(TaskState.ERROR, errorMessage = longMessage))

        assertEquals("Error: ${longMessage.take(50)} · 14:05", result)
    }

    @Test
    fun `formatStateInfo for other states shows only the humanized label without the time`() {
        val result = formatStateInfo(info(TaskState.COMPLETED))

        assertEquals("Completed", result)
    }

    @Test
    fun `formatStateInfo humanizes underscored state names`() {
        val result = formatStateInfo(info(TaskState.STORED_LOCALLY_IN_DESTINATION))

        assertEquals("Stored locally in destination", result)
    }
}
