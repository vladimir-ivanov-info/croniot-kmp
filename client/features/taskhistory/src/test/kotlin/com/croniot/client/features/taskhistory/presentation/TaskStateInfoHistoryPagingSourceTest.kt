package com.croniot.client.features.taskhistory.presentation

import Outcome
import android.util.Log
import androidx.paging.PagingSource
import com.croniot.client.domain.errors.RemoteError
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.TaskStateInfoHistoryEntry
import com.croniot.client.domain.models.TaskType
import com.croniot.client.domain.repositories.TaskTypesRepository
import com.croniot.client.domain.usecases.FetchTaskStateInfoHistoryUseCase
import croniot.models.TaskKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class TaskStateInfoHistoryPagingSourceTest {

    private val fetchTaskStateInfoHistoryUseCase: FetchTaskStateInfoHistoryUseCase = mockk()
    private val taskTypesRepository: TaskTypesRepository = mockk()
    private val deviceUuid = "device-1"
    private val taskTypeA = TaskType(uid = 10L, name = "Watering", description = "", parameters = emptyList())

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun buildSource(pageSize: Int = 10, taskTypeUidFilter: Long? = null) = TaskStateInfoHistoryPagingSource(
        fetchTaskStateInfoHistoryUseCase = fetchTaskStateInfoHistoryUseCase,
        taskTypesRepository = taskTypesRepository,
        deviceUuid = deviceUuid,
        snapshotBefore = "1000000",
        pageSize = pageSize,
        taskTypeUidFilter = taskTypeUidFilter,
    )

    private fun entry(stateInfoId: Long, taskUid: Long = 1L, taskTypeUid: Long = 10L, epochMillis: Long = 500_000L) =
        TaskStateInfoHistoryEntry(
            stateInfoId = stateInfoId,
            taskKey = TaskKey(deviceUuid, taskTypeUid, taskUid),
            stateInfo = TaskStateInfo(
                dateTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), ZonedDateTime.now().zone),
                state = "RUNNING",
                progress = 0.5,
                errorMessage = "",
            ),
        )

    private fun refreshParams(pageSize: Int = 10) =
        PagingSource.LoadParams.Refresh<TaskHistoryCursor>(key = null, loadSize = pageSize, placeholdersEnabled = false)

    @Test
    fun `WHEN load is called with initial params THEN it uses snapshotBefore and Long MAX_VALUE as beforeId`() = runTest {
        coEvery { fetchTaskStateInfoHistoryUseCase(deviceUuid, 10, "1000000", Long.MAX_VALUE, null) } returns Outcome.Ok(emptyList())
        val source = buildSource()

        val result = source.load(refreshParams())

        assertTrue(result is PagingSource.LoadResult.Page)
    }

    @Test
    fun `WHEN load returns entries THEN they are mapped to TaskHistoryItem using the task type name`() = runTest {
        val entries = listOf(entry(stateInfoId = 1L))
        coEvery { fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any()) } returns Outcome.Ok(entries)
        every { taskTypesRepository.get(deviceUuid, 10L) } returns taskTypeA
        val source = buildSource()

        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertEquals(1, result.data.size)
        assertEquals("Watering", result.data.first().taskTypeName)
    }

    @Test
    fun `WHEN the task type is not registered THEN load falls back to Unknown as the task type name`() = runTest {
        val entries = listOf(entry(stateInfoId = 1L))
        coEvery { fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any()) } returns Outcome.Ok(entries)
        every { taskTypesRepository.get(deviceUuid, 10L) } returns null
        val source = buildSource()

        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertEquals("Unknown", result.data.first().taskTypeName)
    }

    @Test
    fun `WHEN load returns fewer items than pageSize THEN nextKey is null`() = runTest {
        val entries = listOf(entry(stateInfoId = 1L))
        coEvery { fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any()) } returns Outcome.Ok(entries)
        every { taskTypesRepository.get(any(), any()) } returns taskTypeA
        val source = buildSource(pageSize = 10)

        val result = source.load(refreshParams(pageSize = 10)) as PagingSource.LoadResult.Page

        assertNull(result.nextKey)
    }

    @Test
    fun `WHEN load returns an empty result THEN nextKey is null and data is empty`() = runTest {
        coEvery { fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any()) } returns Outcome.Ok(emptyList())
        val source = buildSource()

        val result = source.load(refreshParams()) as PagingSource.LoadResult.Page

        assertNull(result.nextKey)
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `WHEN load returns a full page THEN nextKey is based on the last item`() = runTest {
        val entries = List(2) { i -> entry(stateInfoId = (i + 1).toLong(), epochMillis = 500_000L + i) }
        coEvery { fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any()) } returns Outcome.Ok(entries)
        every { taskTypesRepository.get(any(), any()) } returns taskTypeA
        val source = buildSource(pageSize = 2)

        val result = source.load(refreshParams(pageSize = 2)) as PagingSource.LoadResult.Page

        assertEquals(TaskHistoryCursor(before = (500_000L + 1).toString(), beforeId = 2L), result.nextKey)
    }

    @Test
    fun `WHEN the candidate cursor equals the current cursor THEN nextKey is null to avoid an infinite loop`() = runTest {
        // A single-item page whose cursor would be identical to the initial one (guards against stuck pagination)
        val entry = entry(stateInfoId = Long.MAX_VALUE, epochMillis = 1000000L)
        coEvery { fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any()) } returns Outcome.Ok(listOf(entry))
        every { taskTypesRepository.get(any(), any()) } returns taskTypeA
        val source = buildSource(pageSize = 1)

        val result = source.load(refreshParams(pageSize = 1)) as PagingSource.LoadResult.Page

        assertNull(result.nextKey)
    }

    @Test
    fun `WHEN load receives an error outcome THEN it returns LoadResult Error`() = runTest {
        coEvery {
            fetchTaskStateInfoHistoryUseCase(any(), any(), any(), any(), any())
        } returns Outcome.Err(TaskError.Remote(RemoteError.Unreachable))
        val source = buildSource()

        val result = source.load(refreshParams())

        assertTrue(result is PagingSource.LoadResult.Error)
    }

    @Test
    fun `WHEN a taskTypeUidFilter is provided THEN load applies it to the use case call`() = runTest {
        coEvery { fetchTaskStateInfoHistoryUseCase(deviceUuid, 10, "1000000", Long.MAX_VALUE, 99L) } returns Outcome.Ok(emptyList())
        val source = buildSource(taskTypeUidFilter = 99L)

        source.load(refreshParams())

        io.mockk.coVerify(exactly = 1) {
            fetchTaskStateInfoHistoryUseCase(deviceUuid, 10, "1000000", Long.MAX_VALUE, 99L)
        }
    }

    @Test
    fun `WHEN getRefreshKey is called THEN it always returns null`() {
        val source = buildSource()

        val result = source.getRefreshKey(
            androidx.paging.PagingState(
                pages = emptyList(),
                anchorPosition = null,
                config = androidx.paging.PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            ),
        )

        assertNull(result)
    }
}
