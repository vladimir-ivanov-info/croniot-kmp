package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class TaskHistoryFilterTest {

    @Test
    fun `WHEN no criteria is set THEN isActive is false`() {
        val filter = TaskHistoryFilter()

        assertThat(filter.isActive).isFalse()
    }

    @Test
    fun `WHEN taskTypeUids is not empty THEN isActive is true`() {
        val filter = TaskHistoryFilter(taskTypeUids = setOf(1L, 2L))

        assertThat(filter.isActive).isTrue()
    }

    @Test
    fun `WHEN dateFromMillis is set THEN isActive is true`() {
        val filter = TaskHistoryFilter(dateFromMillis = 1_000L)

        assertThat(filter.isActive).isTrue()
    }

    @Test
    fun `WHEN dateToMillis is set THEN isActive is true`() {
        val filter = TaskHistoryFilter(dateToMillis = 2_000L)

        assertThat(filter.isActive).isTrue()
    }

    @Test
    fun `WHEN filter is the NONE companion constant THEN it is inactive and equal to a default TaskHistoryFilter`() {
        assertThat(TaskHistoryFilter.NONE.isActive).isFalse()
        assertThat(TaskHistoryFilter.NONE).isEqualTo(TaskHistoryFilter())
    }
}
