package com.server.croniot.data.repositories

import com.server.croniot.data.db.entities.FeatureFlagEntity
import com.server.croniot.testsupport.fakes.FakeFeatureFlagDao
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

/**
 * Unit tests for [FeatureFlagRepository] using a fake (not a mock) of
 * [com.server.croniot.data.db.daos.FeatureFlagDao]. Every method here is plain delegation, but is
 * still covered against the fake's realistic write-then-read behavior (in particular
 * [FeatureFlagRepository.setEnabled], whose success/failure result depends on whether the flag exists).
 */
class FeatureFlagRepositoryTest {

    private val featureFlagDao = FakeFeatureFlagDao()
    private val repository = FeatureFlagRepository(featureFlagDao)

    @Test
    fun `WHEN flags are seeded THEN getAll returns every one of them, otherwise empty`() {
        assertTrue(repository.getAll().isEmpty())

        featureFlagDao.seed(flag("flag-a"))
        featureFlagDao.seed(flag("flag-b"))

        assertEquals(setOf("flag-a", "flag-b"), repository.getAll().map { it.name }.toSet())
    }

    @Test
    fun `WHEN name is known THEN getByName returns the flag, otherwise null`() {
        featureFlagDao.seed(flag("flag-a"))

        assertEquals("flag-a", repository.getByName("flag-a")?.name)
        assertNull(repository.getByName("missing-flag"))
    }

    @Test
    fun `WHEN flag exists THEN setEnabled updates it and returns true`() {
        featureFlagDao.seed(flag("flag-a", enabled = false))

        val result = repository.setEnabled("flag-a", enabled = true)

        assertTrue(result)
        assertTrue(repository.getByName("flag-a")?.enabled == true)
    }

    @Test
    fun `WHEN flag is unknown THEN setEnabled returns false`() {
        assertFalse(repository.setEnabled("missing-flag", enabled = true))
    }

    private fun flag(name: String, enabled: Boolean = false) = FeatureFlagEntity(
        name = name,
        enabled = enabled,
        description = null,
        updatedAt = OffsetDateTime.now(),
    )
}
