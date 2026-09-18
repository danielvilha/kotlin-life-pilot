package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import javax.inject.Inject

class ReorderTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend operator fun invoke(
        tasks: List<Task>
    ) {
        val reorderedTasks = tasks.mapIndexed { index, task ->
            task.copy(position = index)
        }

        taskRepository.updateTaskOrder(reorderedTasks)
    }
}