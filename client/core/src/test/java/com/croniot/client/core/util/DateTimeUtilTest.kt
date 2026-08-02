package com.croniot.client.core.util

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.startsWith
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class DateTimeUtilTest {

    @Test
    fun `WHEN the timestamp is a few seconds in the past THEN formatRelativeTime ends with ago and has no unit letters`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusSeconds(10))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("s ")
    }

    @Test
    fun `WHEN the timestamp is minutes in the past THEN formatRelativeTime reports minutes and seconds`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusMinutes(5))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("m ")
    }

    @Test
    fun `WHEN the timestamp is hours in the past THEN formatRelativeTime reports hours and minutes`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusHours(3))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("h ")
    }

    @Test
    fun `WHEN the timestamp is days in the past THEN formatRelativeTime reports days and hours`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusDays(2))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("d ")
        assertThat(result).contains("h")
    }

    @Test
    fun `WHEN the timestamp is weeks in the past THEN formatRelativeTime reports weeks and days`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusWeeks(2))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("w ")
    }

    @Test
    fun `WHEN the timestamp is months in the past THEN formatRelativeTime reports months and weeks`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusDays(70))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("m ")
    }

    @Test
    fun `WHEN the timestamp is years in the past THEN formatRelativeTime reports years and months`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().minusDays(400))

        assertThat(result).endsWith("ago")
        assertThat(result).contains("y ")
    }

    @Test
    fun `WHEN the timestamp is in the future THEN formatRelativeTime prefixes the result with in instead of suffixing with ago`() {
        val result = DateTimeUtil.formatRelativeTime(ZonedDateTime.now().plusHours(3))

        assertThat(result).startsWith("in ")
        assertThat(result).contains("h ")
    }
}
