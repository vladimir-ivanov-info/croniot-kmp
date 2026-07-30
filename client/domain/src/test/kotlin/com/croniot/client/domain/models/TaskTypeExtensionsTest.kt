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
    fun `isInstant is true for a single non-time parameter`() {
        val type = taskType(listOf(parameter(ParameterTypes.NUMBER)))

        assertThat(type.isInstant()).isTrue()
    }

    @Test
    fun `isInstant is false for a single time parameter`() {
        val type = taskType(listOf(parameter(ParameterTypes.TIME)))

        assertThat(type.isInstant()).isFalse()
    }

    @Test
    fun `isInstant is false when there is more than one parameter`() {
        val type = taskType(listOf(parameter(ParameterTypes.NUMBER), parameter(ParameterTypes.NUMBER)))

        assertThat(type.isInstant()).isFalse()
    }

    @Test
    fun `isInstant is false when there are no parameters`() {
        val type = taskType(emptyList())

        assertThat(type.isInstant()).isFalse()
    }
}
