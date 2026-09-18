package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    operator fun invoke(): Flow<List<Task>> {
        return taskRepository.observeTasks()
    }
}