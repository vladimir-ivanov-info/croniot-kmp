package com.croniot.client.domain.models

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import croniot.models.ParameterTypes
import org.junit.jupiter.api.Test

class TaskTypeExtensionsTest {

    private fun parameter(type: String) = ParameterTask(
        uid = 1L,
        name = "p",
        type = type,
        unit = "",
        description = "",
        constraints = emptyMap(),
    )

    private fun taskType(parameters: List<ParameterTask>) = TaskType(
        uid = 1L,
        name = "task",
        description = "",
        parameters = parameters,
    )

    @Test
    fun `WHEN there is a single non-time parameter THEN isInstant is true`() {
        val type = taskType(listOf(parameter(ParameterTypes.NUMBER)))

        assertThat(type.isInstant()).isTrue()
    }

    @Test
    fun `WHEN there is a single time parameter THEN isInstant is false`() {
        val type = taskType(listOf(parameter(ParameterTypes.TIME)))

        assertThat(type.isInstant()).isFalse()
    }

    @Test
    fun `WHEN there is more than one parameter THEN isInstant is false`() {
        val type = taskType(listOf(parameter(ParameterTypes.NUMBER), parameter(ParameterTypes.NUMBER)))

        assertThat(type.isInstant()).isFalse()
    }

    @Test
    fun `WHEN there are no parameters THEN isInstant is false`() {
        val type = taskType(emptyList())

        assertThat(type.isInstant()).isFalse()
    }
}
