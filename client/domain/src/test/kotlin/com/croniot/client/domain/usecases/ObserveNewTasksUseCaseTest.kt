package com.croniot.client.domain.usecases

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.croniot.client.domain.models.Task
import com.croniot.testing.fakes.FakeTasksRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObserveNewTasksUseCaseTest {

    @Test
    fun `WHEN repository flow pushes tasks THEN it emits those tasks`() = runTest {
        val task1 = Task(deviceUuid = "device-1", taskTypeUid = 10L, uid = 1L)
        val task2 = Task(deviceUuid = "device-1", taskTypeUid = 10L, uid = 2L)
        val repository = FakeTasksRepository(newTasksFlow = flowOf(task1, task2))
        val useCase = ObserveNewTasksUseCase(repository)

        val result = useCase("device-1").toList()

        assertThat(result).isEqualTo(listOf(task1, task2))
    }

    @Test
    fun `WHEN repository flow is empty THEN it emits nothing`() = runTest {
        val repository = FakeTasksRepository()
        val useCase = ObserveNewTasksUseCase(repository)

        val result = useCase("device-1").toList()

        assertThat(result).isEqualTo(emptyList())
    }
}
