package com.croniot.client.domain.usecases

import com.croniot.client.core.models.Task
import com.croniot.client.domain.errors.TaskError
import com.croniot.client.domain.repositories.TasksRepository
import Outcome

class SendNewTaskUseCaseImpl(
    private val tasksRepository: TasksRepository
) : SendNewTaskUseCase {

    override suspend operator fun invoke(deviceUuid: String, taskTypeUid: Long, parametersValues: Map<Long, String>): Outcome<Unit, TaskError> {
        val newTask = Task(
            deviceUuid = deviceUuid,
            taskTypeUid = taskTypeUid,
            parametersValues = parametersValues.toMutableMap()
        )
        return tasksRepository.sendNewTask(newTask)
    }
}
