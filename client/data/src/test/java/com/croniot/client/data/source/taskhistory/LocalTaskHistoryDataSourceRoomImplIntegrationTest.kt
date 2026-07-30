package com.croniot.client.data.source.taskhistory

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import croniot.models.TaskKey
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZonedDateTime

/**
 * Complements LocalTaskHistoryDataSourceRoomImplTest (mocked TaskHistoryCacheDao) with an
 * end-to-end pass through a real in-memory Room database, exercising savePage's retention logic
 * and getPage's cursor pagination together with the real entity<->domain mapping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalTaskHistoryDataSourceRoomImplIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var dataSource: LocalTaskHistoryDataSourceRoomImpl
    private val deviceUuid = "device-1"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dataSource = LocalTaskHistoryDataSourceRoomImpl(db.taskHistoryCacheDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun historyEntry(stateInfoId: Long, epochSeconds: Long) = TaskStateInfoHistoryEntry(
        stateInfoId = stateInfoId,
        taskKey = TaskKey(deviceUuid, 10L, 1L),
        stateInfo = TaskStateInfo(
            dateTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(epochSeconds), java.time.ZoneOffset.UTC),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        ),
    )

    @Test
    fun `savePage then getPage returns entries ordered by newest first`() = runTest {
        dataSource.savePage(
            deviceUuid,
            listOf(historyEntry(1L, 1000L), historyEntry(2L, 3000L), historyEntry(3L, 2000L)),
        )

        val result = dataSource.getPage(deviceUuid, limit = 10, before = null, beforeId = null)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.stateInfoId })
    }

    @Test
    fun `count reflects the number of saved entries`() = runTest {
        dataSource.savePage(deviceUuid, listOf(historyEntry(1L, 1000L), historyEntry(2L, 2000L)))

        val count = dataSource.count(deviceUuid, before = null, beforeId = null)

        assertEquals(2, count)
    }

    @Test
    fun `savePage with an empty list is a no-op`() = runTest {
        dataSource.savePage(deviceUuid, emptyList())

        assertTrue(dataSource.getPage(deviceUuid, limit = 10, before = null, beforeId = null).isEmpty())
    }

    @Test
    fun `getPage with a numeric before cursor paginates strictly older entries`() = runTest {
        dataSource.savePage(deviceUuid, listOf(historyEntry(1L, 1000L), historyEntry(2L, 2000L)))
        val firstPage = dataSource.getPage(deviceUuid, limit = 1, before = null, beforeId = null)
        val cursorMillis = firstPage.first().stateInfo.dateTime.toInstant().toEpochMilli()

        val secondPage = dataSource.getPage(deviceUuid, limit = 1, before = cursorMillis.toString(), beforeId = firstPage.first().stateInfoId)

        assertEquals(listOf(1L), secondPage.map { it.stateInfoId })
    }
}
