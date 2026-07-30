package com.croniot.client.data.source.taskhistory

import com.croniot.client.data.source.local.database.daos.TaskHistoryCacheDao
import com.croniot.client.data.source.local.database.entities.TaskHistoryCacheEntity
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import croniot.models.TaskKey
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class LocalTaskHistoryDataSourceRoomImplTest {

    private val dao: TaskHistoryCacheDao = mockk()
    private val dataSource = LocalTaskHistoryDataSourceRoomImpl(dao)
    private val deviceUuid = "device-1"

    private fun entity(
        stateInfoId: Long? = 1L,
        id: Long = 5L,
        timeStampMillis: Long = 1000L,
    ) = TaskHistoryCacheEntity(
        id = id,
        deviceUuid = deviceUuid,
        stateInfoId = stateInfoId,
        taskUid = 1L,
        taskTypeUid = 10L,
        timeStampMillis = timeStampMillis,
        state = "RUNNING",
        progress = 0.5,
        errorMessage = "",
    )

    private fun historyEntry(stateInfoId: Long = 1L, epochMillis: Long = 1000L) = TaskStateInfoHistoryEntry(
        stateInfoId = stateInfoId,
        taskKey = TaskKey(deviceUuid, 10L, 1L),
        stateInfo = TaskStateInfo(
            dateTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), java.time.ZoneOffset.UTC),
            state = "RUNNING",
            progress = 0.5,
            errorMessage = "",
        ),
    )

    @Test
    fun `getPage with null before passes null beforeMillis and MAX_VALUE beforeId`() = runTest {
        coEvery { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) } returns listOf(entity())

        val result = dataSource.getPage(deviceUuid, 10, before = null, beforeId = null)

        assertEquals(1, result.size)
        coVerify(exactly = 1) { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) }
    }

    @Test
    fun `getPage with numeric before string parses it as epoch millis`() = runTest {
        coEvery { dao.getPage(deviceUuid, 10, 123456L, 7L) } returns emptyList()

        dataSource.getPage(deviceUuid, 10, before = "123456", beforeId = 7L)

        coVerify(exactly = 1) { dao.getPage(deviceUuid, 10, 123456L, 7L) }
    }

    @Test
    fun `getPage with ISO date before string parses it to epoch millis`() = runTest {
        val isoDate = "2024-01-01T00:00:00Z"
        val expectedMillis = OffsetDateTime.parse(isoDate).toInstant().toEpochMilli()
        coEvery { dao.getPage(deviceUuid, 10, expectedMillis, Long.MAX_VALUE) } returns emptyList()

        dataSource.getPage(deviceUuid, 10, before = isoDate, beforeId = null)

        coVerify(exactly = 1) { dao.getPage(deviceUuid, 10, expectedMillis, Long.MAX_VALUE) }
    }

    @Test
    fun `getPage with unparseable before string passes null beforeMillis`() = runTest {
        coEvery { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) } returns emptyList()

        dataSource.getPage(deviceUuid, 10, before = "not-a-date", beforeId = null)

        coVerify(exactly = 1) { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) }
    }

    @Test
    fun `getPage with blank before string passes null beforeMillis`() = runTest {
        coEvery { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) } returns emptyList()

        dataSource.getPage(deviceUuid, 10, before = "  ", beforeId = null)

        coVerify(exactly = 1) { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) }
    }

    @Test
    fun `getPage maps entity with stateInfoId to domain using that id`() = runTest {
        coEvery { dao.getPage(any(), any(), any(), any()) } returns listOf(entity(stateInfoId = 42L, id = 5L))

        val result = dataSource.getPage(deviceUuid, 10, null, null)

        assertEquals(42L, result.first().stateInfoId)
    }

    @Test
    fun `getPage maps entity without stateInfoId to a negative synthetic id based on row id`() = runTest {
        coEvery { dao.getPage(any(), any(), any(), any()) } returns listOf(entity(stateInfoId = null, id = 7L))

        val result = dataSource.getPage(deviceUuid, 10, null, null)

        assertEquals(-7L, result.first().stateInfoId)
    }

    @Test
    fun `savePage with empty entries does nothing`() = runTest {
        dataSource.savePage(deviceUuid, emptyList())

        coVerify(exactly = 0) { dao.insertAll(any()) }
    }

    @Test
    fun `savePage under the max cache size inserts and prunes`() = runTest {
        coEvery { dao.countByDevice(deviceUuid) } returns 10
        coJustRun { dao.insertAll(any()) }
        coJustRun { dao.deleteOldest(any(), any()) }

        dataSource.savePage(deviceUuid, listOf(historyEntry()))

        coVerify(exactly = 1) { dao.insertAll(any()) }
        coVerify(exactly = 1) { dao.deleteOldest(deviceUuid, 1_000) }
    }

    @Test
    fun `savePage at max cache size with newer incoming entries still inserts`() = runTest {
        coEvery { dao.countByDevice(deviceUuid) } returns 1_000
        coEvery { dao.oldestTimestamp(deviceUuid) } returns 500L
        coJustRun { dao.insertAll(any()) }
        coJustRun { dao.deleteOldest(any(), any()) }

        dataSource.savePage(deviceUuid, listOf(historyEntry(epochMillis = 1000L)))

        coVerify(exactly = 1) { dao.insertAll(any()) }
    }

    @Test
    fun `savePage at max cache size with only older or equal incoming entries skips the write`() = runTest {
        coEvery { dao.countByDevice(deviceUuid) } returns 1_000
        coEvery { dao.oldestTimestamp(deviceUuid) } returns 1000L

        dataSource.savePage(deviceUuid, listOf(historyEntry(epochMillis = 500L)))

        coVerify(exactly = 0) { dao.insertAll(any()) }
        coVerify(exactly = 0) { dao.deleteOldest(any(), any()) }
    }

    @Test
    fun `count delegates to dao with parsed before values`() = runTest {
        coEvery { dao.count(deviceUuid, 123L, 5L) } returns 3

        val result = dataSource.count(deviceUuid, before = "123", beforeId = 5L)

        assertEquals(3, result)
    }

    @Test
    fun `count with no before defaults to null millis and MAX_VALUE id`() = runTest {
        coEvery { dao.count(deviceUuid, null, Long.MAX_VALUE) } returns 0

        val result = dataSource.count(deviceUuid, before = null, beforeId = null)

        assertEquals(0, result)
    }

    @Test
    fun `getPage uses the interface default parameters when before and beforeId are omitted`() = runTest {
        coEvery { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) } returns emptyList()

        val result = dataSource.getPage(deviceUuid, 10)

        assertEquals(emptyList<TaskStateInfoHistoryEntry>(), result)
        coVerify(exactly = 1) { dao.getPage(deviceUuid, 10, null, Long.MAX_VALUE) }
    }

    @Test
    fun `count uses the interface default parameters when before and beforeId are omitted`() = runTest {
        coEvery { dao.count(deviceUuid, null, Long.MAX_VALUE) } returns 0

        val result = dataSource.count(deviceUuid)

        assertEquals(0, result)
        coVerify(exactly = 1) { dao.count(deviceUuid, null, Long.MAX_VALUE) }
    }
}
