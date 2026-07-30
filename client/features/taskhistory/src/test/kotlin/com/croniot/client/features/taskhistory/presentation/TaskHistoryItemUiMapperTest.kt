package com.croniot.client.features.taskhistory.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TaskHistoryItemUiMapperTest {

    private fun item(state: String, progress: Double = 0.0, errorMessage: String = "") = buildTaskHistoryItem(
        stateInfoId = 1L,
        taskUid = 1L,
        taskTypeUid = 10L,
        taskTypeName = "Watering",
        dateTime = ZonedDateTime.now(),
        state = state,
        progress = progress,
        errorMessage = errorMessage,
    )

    @Test
    fun `RUNNING state produces a subtitle with rounded percentage`() {
        val result = item(state = "RUNNING", progress = 42.7)

        assertEquals("Running - 42%", result.subtitle)
    }

    @Test
    fun `ERROR state produces a subtitle prefixed with Error`() {
        val result = item(state = "ERROR", errorMessage = "Sensor timeout")

        assertEquals("Error: Sensor timeout", result.subtitle)
    }

    @Test
    fun `ERROR state truncates a long error message to 50 characters`() {
        val longMessage = "a".repeat(100)

        val result = item(state = "ERROR", errorMessage = longMessage)

        assertEquals("Error: " + "a".repeat(50), result.subtitle)
    }

    @Test
    fun `COMPLETED state produces a plain humanized label without progress or error prefix`() {
        val result = item(state = "COMPLETED")

        assertEquals("Completed", result.subtitle)
    }

    @Test
    fun `state label replaces underscores with spaces and capitalizes the first letter`() {
        val result = item(state = "PENDING_SYNC")

        assertEquals("Pending sync", result.subtitle)
    }

    @Test
    fun `time field formats dateTime using HH mm pattern`() {
        val dateTime = ZonedDateTime.now().withHour(14).withMinute(5)

        val result = buildTaskHistoryItem(
            stateInfoId = 1L,
            taskUid = 1L,
            taskTypeUid = 10L,
            taskTypeName = "Watering",
            dateTime = dateTime,
            state = "COMPLETED",
            progress = 0.0,
            errorMessage = "",
        )

        assertEquals("14:05", result.time)
    }

    @Test
    fun `relativeTime is a non-empty human readable string`() {
        val result = item(state = "COMPLETED")

        assertTrue(result.relativeTime.isNotEmpty())
    }

    @Test
    fun `preserves the stateInfoId taskUid taskTypeUid and taskTypeName as given`() {
        val result = buildTaskHistoryItem(
            stateInfoId = 42L,
            taskUid = 7L,
            taskTypeUid = 99L,
            taskTypeName = "Lighting",
            dateTime = ZonedDateTime.now(),
            state = "COMPLETED",
            progress = 0.0,
            errorMessage = "",
        )

        assertEquals(42L, result.stateInfoId)
        assertEquals(7L, result.taskUid)
        assertEquals(99L, result.taskTypeUid)
        assertEquals("Lighting", result.taskTypeName)
    }
}
