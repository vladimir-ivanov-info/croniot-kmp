package com.croniot.client.data.source.local.database.daos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.croniot.client.data.source.local.database.AppDatabase
import com.croniot.client.data.source.local.database.entities.TaskHistoryCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TaskHistoryCacheDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TaskHistoryCacheDao

    private fun entity(
        deviceUuid: String = "device-1",
        stateInfoId: Long? = null,
        taskUid: Long = 1L,
        taskTypeUid: Long = 10L,
        timeStampMillis: Long,
        state: String = "RUNNING",
    ) = TaskHistoryCacheEntity(
        deviceUuid = deviceUuid,
        stateInfoId = stateInfoId,
        taskUid = taskUid,
        taskTypeUid = taskTypeUid,
        timeStampMillis = timeStampMillis,
        state = state,
        progress = 0.5,
        errorMessage = "",
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskHistoryCacheDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `WHEN cursor is null THEN getPage returns entries ordered by timestamp descending`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 3000L),
                entity(stateInfoId = 3L, timeStampMillis = 2000L),
            ),
        )

        val result = dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN entries exist for other devices THEN getPage only returns those for the requested device`() = runTest {
        dao.insertAll(listOf(entity(deviceUuid = "device-1", stateInfoId = 1L, timeStampMillis = 1000L)))
        dao.insertAll(listOf(entity(deviceUuid = "device-2", stateInfoId = 2L, timeStampMillis = 1000L)))

        val result = dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE)

        assertEquals(1, result.size)
        assertEquals("device-1", result.first().deviceUuid)
    }

    @Test
    fun `WHEN a real cursor is given THEN getPage excludes the cursor row and anything newer`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, timeStampMillis = 3000L),
            ),
        )

        // cursor = the row with stateInfoId=2 (ts=2000): only strictly-older rows should come back
        val result = dao.getPage("device-1", limit = 10, beforeMillis = 2000L, beforeId = 2L)

        assertEquals(listOf(1L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN beforeMillis is set and beforeId is MAX_VALUE THEN getPage also includes same-timestamp rows with a smaller id`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, timeStampMillis = 3000L),
            ),
        )

        val result = dao.getPage("device-1", limit = 10, beforeMillis = 2000L, beforeId = Long.MAX_VALUE)

        // ts=2000 row also matches: its stateInfoId (2) is less than MAX_VALUE, so the second OR clause admits it too
        assertEquals(listOf(2L, 1L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN entries share the same timestamp THEN getPage paginates using both beforeMillis and beforeId`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 1000L),
                entity(stateInfoId = 3L, timeStampMillis = 1000L),
            ),
        )

        val result = dao.getPage("device-1", limit = 10, beforeMillis = 1000L, beforeId = 2L)

        assertEquals(listOf(1L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN more entries exist than the limit THEN getPage respects the limit`() = runTest {
        dao.insertAll((1..5).map { entity(stateInfoId = it.toLong(), timeStampMillis = it.toLong()) })

        val result = dao.getPage("device-1", limit = 2, beforeMillis = null, beforeId = Long.MAX_VALUE)

        assertEquals(2, result.size)
    }

    @Test
    fun `WHEN an entry has no stateInfoId THEN getPage orders it using its negative row id`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = null, timeStampMillis = 1000L),
                entity(stateInfoId = 5L, timeStampMillis = 1000L),
            ),
        )

        val result = dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE)

        // stateInfoId=5 sorts before the null-stateInfoId row (whose effective sort key is -id, always negative)
        assertEquals(5L, result.first().stateInfoId)
    }

    @Test
    fun `WHEN getPage is called without a limit THEN count matches the number of entries returned`() = runTest {
        dao.insertAll((1..3).map { entity(stateInfoId = it.toLong(), timeStampMillis = it.toLong()) })

        val count = dao.count("device-1", beforeMillis = null, beforeId = Long.MAX_VALUE)

        assertEquals(3, count)
    }

    @Test
    fun `WHEN counting by device THEN countByDevice counts all entries regardless of cursor`() = runTest {
        dao.insertAll((1..4).map { entity(stateInfoId = it.toLong(), timeStampMillis = it.toLong()) })

        assertEquals(4, dao.countByDevice("device-1"))
    }

    @Test
    fun `WHEN a device has multiple entries THEN oldestTimestamp returns the minimum timeStampMillis`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 3000L),
                entity(stateInfoId = 2L, timeStampMillis = 1000L),
            ),
        )

        assertEquals(1000L, dao.oldestTimestamp("device-1"))
    }

    @Test
    fun `WHEN the device has no entries THEN oldestTimestamp returns null`() = runTest {
        assertEquals(null, dao.oldestTimestamp("device-1"))
    }

    @Test
    fun `WHEN deleteOldest is called with maxEntries THEN it keeps only the most recent rows`() = runTest {
        dao.insertAll((1..5).map { entity(stateInfoId = it.toLong(), timeStampMillis = it.toLong()) })

        dao.deleteOldest("device-1", maxEntries = 3)

        val remaining = dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE)
        assertEquals(listOf(5L, 4L, 3L), remaining.map { it.stateInfoId })
    }

    @Test
    fun `WHEN deleteOldest is called for one device THEN it does not affect other devices`() = runTest {
        dao.insertAll(listOf(entity(deviceUuid = "device-1", stateInfoId = 1L, timeStampMillis = 1000L)))
        dao.insertAll(listOf(entity(deviceUuid = "device-2", stateInfoId = 2L, timeStampMillis = 1000L)))

        dao.deleteOldest("device-1", maxEntries = 0)

        assertTrue(dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE).isEmpty())
        assertEquals(1, dao.getPage("device-2", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE).size)
    }

    @Test
    fun `WHEN deviceUuid and stateInfoId are duplicated THEN insertAll ignores the second insert`() = runTest {
        dao.insertAll(listOf(entity(stateInfoId = 1L, timeStampMillis = 1000L, state = "RUNNING")))
        dao.insertAll(listOf(entity(stateInfoId = 1L, timeStampMillis = 1000L, state = "COMPLETED")))

        val result = dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE)

        assertEquals(1, result.size)
        assertEquals("RUNNING", result.first().state)
    }

    @Test
    fun `WHEN stateInfoIds are different THEN insertAll inserts every row`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
            ),
        )

        val result = dao.getPage("device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE)

        assertEquals(2, result.size)
    }

    @Test
    fun `WHEN a taskTypeUid filter set is given THEN getPageFilteredByTaskTypes only returns entries whose taskTypeUid is in it`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, taskTypeUid = 20L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, taskTypeUid = 30L, timeStampMillis = 3000L),
            ),
        )

        val result = dao.getPageFilteredByTaskTypes(
            "device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE,
            taskTypeUids = listOf(10L, 30L), dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(setOf(1L, 3L), result.map { it.stateInfoId }.toSet())
    }

    @Test
    fun `WHEN a date range is given alongside the type filter THEN getPageFilteredByTaskTypes applies both`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, taskTypeUid = 10L, timeStampMillis = 5000L),
            ),
        )

        val result = dao.getPageFilteredByTaskTypes(
            "device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE,
            taskTypeUids = listOf(10L), dateFromMillis = 2000L, dateToMillis = 6000L,
        )

        assertEquals(listOf(2L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN a cursor and limit are given THEN getPageFilteredByTaskTypes respects both`() = runTest {
        dao.insertAll(
            (1..5).map { entity(stateInfoId = it.toLong(), taskTypeUid = 10L, timeStampMillis = it.toLong() * 1000) },
        )

        val result = dao.getPageFilteredByTaskTypes(
            "device-1", limit = 2, beforeMillis = null, beforeId = Long.MAX_VALUE,
            taskTypeUids = listOf(10L), dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(2, result.size)
        assertEquals(listOf(5L, 4L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN a type filter is given THEN countFilteredByTaskTypes counts only matching entries`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, taskTypeUid = 20L, timeStampMillis = 2000L),
            ),
        )

        val count = dao.countFilteredByTaskTypes(
            "device-1", beforeMillis = null, beforeId = Long.MAX_VALUE,
            taskTypeUids = listOf(10L), dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(1, count)
    }

    @Test
    fun `WHEN a date range is given THEN getPageFilteredByDates returns entries within it regardless of task type`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, taskTypeUid = 20L, timeStampMillis = 5000L),
                entity(stateInfoId = 3L, taskTypeUid = 30L, timeStampMillis = 9000L),
            ),
        )

        val result = dao.getPageFilteredByDates(
            "device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE,
            dateFromMillis = 2000L, dateToMillis = 6000L,
        )

        assertEquals(listOf(2L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN date bounds are null THEN getPageFilteredByDates behaves like an unfiltered date range`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
            ),
        )

        val result = dao.getPageFilteredByDates(
            "device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE,
            dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `WHEN a date range is given THEN countFilteredByDates counts only entries within it`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 5000L),
                entity(stateInfoId = 3L, timeStampMillis = 9000L),
            ),
        )

        val count = dao.countFilteredByDates(
            "device-1", beforeMillis = null, beforeId = Long.MAX_VALUE,
            dateFromMillis = 2000L, dateToMillis = 6000L,
        )

        assertEquals(1, count)
    }

    @Test
    fun `WHEN entries exist for other devices THEN getPageFilteredByTaskTypes only returns those for the requested device`() = runTest {
        dao.insertAll(listOf(entity(deviceUuid = "device-1", stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L)))
        dao.insertAll(listOf(entity(deviceUuid = "device-2", stateInfoId = 2L, taskTypeUid = 10L, timeStampMillis = 1000L)))

        val result = dao.getPageFilteredByTaskTypes(
            "device-1", limit = 10, beforeMillis = null, beforeId = Long.MAX_VALUE,
            taskTypeUids = listOf(10L), dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(1, result.size)
    }

    // --- Cursor (beforeMillis non-null) on the methods that only ever got exercised with beforeMillis = null above ---

    @Test
    fun `WHEN a real cursor is given THEN count excludes the cursor row and anything newer`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, timeStampMillis = 3000L),
            ),
        )

        val count = dao.count("device-1", beforeMillis = 2000L, beforeId = 2L)

        assertEquals(1, count)
    }

    @Test
    fun `WHEN a real cursor is given THEN getPageFilteredByTaskTypes excludes the cursor row and anything newer`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, taskTypeUid = 10L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, taskTypeUid = 10L, timeStampMillis = 3000L),
            ),
        )

        val result = dao.getPageFilteredByTaskTypes(
            "device-1", limit = 10, beforeMillis = 2000L, beforeId = 2L,
            taskTypeUids = listOf(10L), dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(listOf(1L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN a real cursor and a real date range are given THEN countFilteredByTaskTypes counts only the matching older rows`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, taskTypeUid = 10L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, taskTypeUid = 10L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, taskTypeUid = 10L, timeStampMillis = 3000L),
            ),
        )

        val count = dao.countFilteredByTaskTypes(
            "device-1", beforeMillis = 3000L, beforeId = 3L,
            taskTypeUids = listOf(10L), dateFromMillis = 500L, dateToMillis = 2500L,
        )

        assertEquals(2, count)
    }

    @Test
    fun `WHEN a real cursor is given THEN getPageFilteredByDates excludes the cursor row and anything newer`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, timeStampMillis = 3000L),
            ),
        )

        val result = dao.getPageFilteredByDates(
            "device-1", limit = 10, beforeMillis = 2000L, beforeId = 2L,
            dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(listOf(1L), result.map { it.stateInfoId })
    }

    @Test
    fun `WHEN a real cursor is given and date bounds are null THEN countFilteredByDates counts only the older rows`() = runTest {
        dao.insertAll(
            listOf(
                entity(stateInfoId = 1L, timeStampMillis = 1000L),
                entity(stateInfoId = 2L, timeStampMillis = 2000L),
                entity(stateInfoId = 3L, timeStampMillis = 3000L),
            ),
        )

        val count = dao.countFilteredByDates(
            "device-1", beforeMillis = 3000L, beforeId = 3L,
            dateFromMillis = null, dateToMillis = null,
        )

        assertEquals(2, count)
    }
}
