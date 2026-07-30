package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class TaskHistoryFilterTest {

    @Test
    fun `isActive is false when no criteria is set`() {
        val filter = TaskHistoryFilter()

        assertThat(filter.isActive).isFalse()
    }

    @Test
    fun `isActive is true when taskTypeUids is not empty`() {
        val filter = TaskHistoryFilter(taskTypeUids = setOf(1L, 2L))

        assertThat(filter.isActive).isTrue()
    }

    @Test
    fun `isActive is true when dateFromMillis is set`() {
        val filter = TaskHistoryFilter(dateFromMillis = 1_000L)

        assertThat(filter.isActive).isTrue()
    }

    @Test
    fun `isActive is true when dateToMillis is set`() {
        val filter = TaskHistoryFilter(dateToMillis = 2_000L)

        assertThat(filter.isActive).isTrue()
    }

    @Test
    fun `NONE companion constant is an inactive filter`() {
        assertThat(TaskHistoryFilter.NONE.isActive).isFalse()
        assertThat(TaskHistoryFilter.NONE).isEqualTo(TaskHistoryFilter())
    }
}
