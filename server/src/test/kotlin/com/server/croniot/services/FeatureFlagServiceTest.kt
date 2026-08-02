package com.server.croniot.services

import com.server.croniot.application.DomainException
import com.server.croniot.data.db.entities.FeatureFlagEntity
import com.server.croniot.data.repositories.FeatureFlagRepository
import croniot.models.errors.DomainError
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class FeatureFlagServiceTest {

    private val repo: FeatureFlagRepository = mockk()
    private val service = FeatureFlagService(repo)

    private fun entity(
        name: String = "new_ui",
        enabled: Boolean = true,
        description: String? = "Enables the new UI",
    ) = FeatureFlagEntity(
        name = name,
        enabled = enabled,
        description = description,
        updatedAt = OffsetDateTime.parse("2026-04-19T10:00:00Z"),
    )

    @Test
    fun `WHEN getAll is called THEN it maps every repository entity to a dto`() {
        every { repo.getAll() } returns listOf(
            entity(name = "flag_a", enabled = true),
            entity(name = "flag_b", enabled = false, description = null),
        )

        val result = service.getAll()

        assertEquals(2, result.size)
        assertEquals("flag_a", result[0].name)
        assertTrue(result[0].enabled)
        assertEquals("Enables the new UI", result[0].description)
        assertEquals("flag_b", result[1].name)
        assertTrue(!result[1].enabled)
        assertEquals(null, result[1].description)
    }

    @Test
    fun `WHEN there are no flags THEN getAll returns an empty list`() {
        every { repo.getAll() } returns emptyList()

        assertTrue(service.getAll().isEmpty())
    }

    @Test
    fun `WHEN the flag exists THEN setEnabled updates it and returns the refreshed dto`() {
        every { repo.setEnabled("new_ui", true) } returns true
        every { repo.getByName("new_ui") } returns entity(name = "new_ui", enabled = true)

        val result = service.setEnabled("new_ui", true)

        assertEquals("new_ui", result.name)
        assertTrue(result.enabled)
        verify(exactly = 1) { repo.setEnabled("new_ui", true) }
    }

    @Test
    fun `WHEN the flag does not exist THEN setEnabled throws NotFound`() {
        every { repo.setEnabled("unknown", true) } returns false

        val ex = assertThrows(DomainException::class.java) { service.setEnabled("unknown", true) }

        assertInstanceOf(DomainError.NotFound::class.java, ex.error)
        assertEquals("feature_flag 'unknown' not found", ex.error.message)
        verify(exactly = 0) { repo.getByName(any()) }
    }
}
