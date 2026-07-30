package croniot

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class TimeUtilTest {

    @Test
    fun `measure returns the value produced by the block`() {
        val result = measure("test-block") { 40 + 2 }

        assertThat(result).isEqualTo(42)
    }

    @Test
    fun `measure invokes the provided log callback instead of printing`() {
        var loggedMessage: String? = null

        measure("test-block", log = { loggedMessage = it }) { "ignored" }

        assertThat(loggedMessage).isNotNull()
    }

    @Test
    fun `measure log message includes the block name`() {
        var loggedMessage = ""

        measure("my-operation", log = { loggedMessage = it }) { Unit }

        assertThat(loggedMessage.startsWith("my-operation took")).isTrue()
    }
}
