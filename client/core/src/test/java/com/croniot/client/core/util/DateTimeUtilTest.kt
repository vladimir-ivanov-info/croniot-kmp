package com.croniot.client.core.util

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.startsWith
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class DateTimeUtilTest {

    @Test
    fun `formatRelativeTime for a few seconds in the past ends with ago and has no unit letters`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusSeconds(10))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("s ")
    }

    @Test
    fun `formatRelativeTime for minutes in the past reports minutes and seconds`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusMinutes(5))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("m ")
    }

    @Test
    fun `formatRelativeTime for hours in the past reports hours and minutes`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusHours(3))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("h ")
    }

    @Test
    fun `formatRelativeTime for days in the past reports days and hours`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusDays(2))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("d ")
        assertThat(result).contains("h")
    }

    @Test
    fun `formatRelativeTime for weeks in the past reports weeks and days`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusWeeks(2))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("w ")
    }

    @Test
    fun `formatRelativeTime for months in the past reports months and weeks`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusDays(70))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("m ")
    }

    @Test
    fun `formatRelativeTime for years in the past reports years and months`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusDays(400))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("y ")
    }

    @Test
    fun `formatRelativeTime for a future instant is prefixed with in instead of suffixed with ago`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().plusHours(3))

        assertThat(result).startsWith("in ")
        assertThat(result).contains("h ")
    }
}
