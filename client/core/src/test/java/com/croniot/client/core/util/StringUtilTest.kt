package com.croniot.client.core.util

import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.jupiter.api.Test

class StringUtilTest {

    @Test
    fun `generateRandomString returns a string of the requested length`() {
        assertThat(StringUtil.generateRandomString(length = 8)).hasLength(8)
    }

    @Test
    fun `generateRandomString uses the default length of 3 when none is provided`() {
        assertThat(StringUtil.generateRandomString()).hasLength(3)
    }

    @Test
    fun `generateUniqueString truncates the uuid to the requested length`() {
        assertThat(StringUtil.generateUniqueString(length = 6)).hasLength(6)
    }

    @Test
    fun `generateUniqueString produces different values on each call`() {
        val first = StringUtil.generateUniqueString(length = 20)
        val second = StringUtil.generateUniqueString(length = 20)

        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `getRelativeTimeText returns a fallback message when last is null`() {
        assertThat(getRelativeTimeText(now = 1_000L, last = null)).isEqualTo("no recent signal")
    }

    @Test
    fun `getRelativeTimeText returns a fallback message when last is not positive`() {
        assertThat(getRelativeTimeText(now = 1_000L, last = 0L)).isEqualTo("no recent signal")
    }

    @Test
    fun `getRelativeTimeText reports real time for gaps under 5 seconds`() {
        assertThat(getRelativeTimeText(now = 4_000L, last = 1_000L)).isEqualTo("in real time")
    }

    @Test
    fun `getRelativeTimeText reports seconds for gaps under a minute`() {
        assertThat(getRelativeTimeText(now = 21_000L, last = 1_000L)).isEqualTo("20s ago")
    }

    @Test
    fun `getRelativeTimeText reports minutes for gaps under an hour`() {
        assertThat(getRelativeTimeText(now = 121_000L, last = 1_000L)).isEqualTo("2 min ago")
    }

    @Test
    fun `getRelativeTimeText reports hours for gaps of an hour or more`() {
        assertThat(getRelativeTimeText(now = 7_201_000L, last = 1_000L)).isEqualTo("2 h ago")
    }
}
