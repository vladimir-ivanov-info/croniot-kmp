package croniot.models

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class TaskStateTest {

    @Test
    fun `WHEN name is a known enum value THEN fromString resolves it`() {
        assertThat(TaskState.fromString("RUNNING")).isEqualTo(TaskState.RUNNING)
    }

    @Test
    fun `WHEN value is unknown THEN fromString falls back to UNDEFINED`() {
        assertThat(TaskState.fromString("NOT_A_REAL_STATE")).isEqualTo(TaskState.UNDEFINED)
    }

    @Test
    fun `WHEN input is lowercase THEN fromString falls back to UNDEFINED (case sensitive)`() {
        assertThat(TaskState.fromString("running")).isEqualTo(TaskState.UNDEFINED)
    }
}
