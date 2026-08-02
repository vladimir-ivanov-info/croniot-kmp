package croniot

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class TimeUtilTest {

    @Test
    fun `WHEN measure wraps a block THEN it returns the block's result`() {
        val result = measure("test") { 42 }

        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `WHEN a custom log callback is provided THEN measure invokes it with a message containing the name`() {
        var loggedMessage: String? = null

        measure("myOperation", log = { loggedMessage = it }) { "done" }

        assertThat(loggedMessage?.contains("myOperation")).isEqualTo(true)
    }

    @Test
    fun `WHEN no log callback is provided THEN measure still returns the block's result`() {
        val result = measure("noLogger", log = null) { listOf(1, 2, 3) }

        assertThat(result).isEqualTo(listOf(1, 2, 3))
    }
}
