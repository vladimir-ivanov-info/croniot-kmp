package com.croniot.client.data.source.local

import androidx.test.core.app.ApplicationProvider
import croniot.models.dto.FeatureFlagDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Same `preferencesDataStore` process-level caching caveat as DataStoreControllerTest: only
 * write-then-read-back assertions, no "empty before anything saved" checks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FeatureFlagLocalDatasourceTest {

    private fun datasource() = FeatureFlagLocalDatasource(ApplicationProvider.getApplicationContext())

    @Test
    fun `WHEN saveFlags is called THEN observeFlags emits the saved flags`() = runTest {
        val ds = datasource()
        val flags = listOf(FeatureFlagDto(name = "new_ui", enabled = true), FeatureFlagDto(name = "beta", enabled = false))

        ds.saveFlags(flags)

        assertEquals(flags, ds.observeFlags().first())
    }

    @Test
    fun `WHEN updateFlag is called for a matching flag THEN it toggles it and leaves others untouched`() = runTest {
        val ds = datasource()
        ds.saveFlags(listOf(FeatureFlagDto(name = "new_ui", enabled = false), FeatureFlagDto(name = "beta", enabled = false)))

        ds.updateFlag("new_ui", enabled = true)

        val flags = ds.observeFlags().first()
        assertTrue(flags.first { it.name == "new_ui" }.enabled)
        assertTrue(!flags.first { it.name == "beta" }.enabled)
    }

    @Test
    fun `WHEN updateFlag is called with a name that matches nothing THEN the saved flags remain unchanged`() = runTest {
        val ds = datasource()
        val flags = listOf(FeatureFlagDto(name = "new_ui", enabled = false))
        ds.saveFlags(flags)

        ds.updateFlag("does_not_exist", enabled = true)

        assertEquals(flags, ds.observeFlags().first())
    }

    // `updateFlag`'s "no flags saved yet" branch (current == null -> emptyList()) is not covered:
    // per the class-level caveat above, this DataStore is process-cached across tests, so a
    // "nothing saved yet" precondition can't be guaranteed here without the same flakiness
    // hallazgo 14 already ruled out for DataStoreControllerTest.
}
