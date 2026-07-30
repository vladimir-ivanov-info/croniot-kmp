package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.models.TaskStateInfo
import com.croniot.client.domain.models.events.TaskStateInfoEvent
import com.croniot.testing.fakes.FakeTasksRepository
import croniot.models.TaskKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ObserveTaskStateInfoUseCaseTest {

    private fun stateInfo(state: String) =
        TaskStateInfo(dateTime = ZonedDateTime.now(), state = state, progress = 0.0, errorMessage = "")

    @Test
    fun `emits mapped state info for the requested device`() = runTest {
        val event = TaskStateInfoEvent(key = TaskKey("device-1", 10L, 1L), info = stateInfo("RUNNING"))
        val repository = FakeTasksRepository(taskStateInfoEventsFlow = flowOf(event))
        val useCase = ObserveTaskStateInfoUseCase(repository)

        val result = useCase("device-1").toList()

        assertThat(result).isEqualTo(listOf(event.info))
    }

    @Test
    fun `filters events by taskTypeUid when provided`() = runTest {
        val matching = TaskStateInfoEvent(key = TaskKey("device-1", 10L, 1L), info = stateInfo("RUNNING"))
        val other = TaskStateInfoEvent(key = TaskKey("device-1", 20L, 2L), info = stateInfo("PENDING"))
        val repository = FakeTasksRepository(taskStateInfoEventsFlow = flowOf(matching, other))
        val useCase = ObserveTaskStateInfoUseCase(repository)

        val result = useCase("device-1", taskTypeUid = 10L).toList()

        assertThat(result).isEqualTo(listOf(matching.info))
    }

    @Test
    fun `passes through all events when taskTypeUid is null`() = runTest {
        val event1 = TaskStateInfoEvent(key = TaskKey("device-1", 10L, 1L), info = stateInfo("RUNNING"))
        val event2 = TaskStateInfoEvent(key = TaskKey("device-1", 20L, 2L), info = stateInfo("PENDING"))
        val repository = FakeTasksRepository(taskStateInfoEventsFlow = flowOf(event1, event2))
        val useCase = ObserveTaskStateInfoUseCase(repository)

        val result = useCase("device-1", taskTypeUid = null).toList()

        assertThat(result).isEqualTo(listOf(event1.info, event2.info))
    }
}
