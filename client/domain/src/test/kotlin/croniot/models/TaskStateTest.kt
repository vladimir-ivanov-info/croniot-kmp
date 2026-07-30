package croniot.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class TaskStateTest {

    @Test
    fun `fromString resolves a known enum name`() {
        assertThat(TaskState.fromString("RUNNING")).isEqualTo(TaskState.RUNNING)
    }

    @Test
    fun `fromString falls back to UNDEFINED for an unknown value`() {
        assertThat(TaskState.fromString("NOT_A_REAL_STATE")).isEqualTo(TaskState.UNDEFINED)
    }

    @Test
    fun `fromString is case sensitive and falls back to UNDEFINED for lowercase input`() {
        assertThat(TaskState.fromString("running")).isEqualTo(TaskState.UNDEFINED)
    }
}
