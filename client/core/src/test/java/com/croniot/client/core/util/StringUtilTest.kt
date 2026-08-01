package com.croniot.client.core.util

import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.jupiter.api.Test

class StringUtilTest {

    @Test
    fun `WHEN a length is requested THEN generateRandomString returns a string of that length`() {
        assertThat(StringUtil.generateRandomString(length = 8)).hasLength(8)
    }

    @Test
    fun `WHEN no length is provided THEN generateRandomString uses the default length of 3`() {
        assertThat(StringUtil.generateRandomString()).hasLength(3)
    }

    @Test
    fun `WHEN a length is requested THEN generateUniqueString truncates the uuid to that length`() {
        assertThat(StringUtil.generateUniqueString(length = 6)).hasLength(6)
    }

    @Test
    fun `WHEN generateUniqueString is called twice THEN it produces different values`() {
        val first = StringUtil.generateUniqueString(length = 20)
        val second = StringUtil.generateUniqueString(length = 20)

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `WHEN last is null THEN getRelativeTimeText returns a fallback message`() {
        assertThat(getRelativeTimeText(now = 1_000L, last = null)).isEqualTo("no recent signal")
    }

    @Test
    fun `WHEN last is not positive THEN getRelativeTimeText returns a fallback message`() {
        assertThat(getRelativeTimeText(now = 1_000L, last = 0L)).isEqualTo("no recent signal")
    }

    @Test
    fun `WHEN the gap is under 5 seconds THEN getRelativeTimeText reports real time`() {
        assertThat(getRelativeTimeText(now = 4_000L, last = 1_000L)).isEqualTo("in real time")
    }

    @Test
    fun `WHEN the gap is under a minute THEN getRelativeTimeText reports it in seconds`() {
        assertThat(getRelativeTimeText(now = 21_000L, last = 1_000L)).isEqualTo("20s ago")
    }

    @Test
    fun `WHEN the gap is under an hour THEN getRelativeTimeText reports it in minutes`() {
        assertThat(getRelativeTimeText(now = 121_000L, last = 1_000L)).isEqualTo("2 min ago")
    }

    @Test
    fun `WHEN the gap is an hour or more THEN getRelativeTimeText reports it in hours`() {
        assertThat(getRelativeTimeText(now = 7_201_000L, last = 1_000L)).isEqualTo("2 h ago")
    }
}
