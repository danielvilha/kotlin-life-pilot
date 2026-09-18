package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend operator fun invoke(task: Task) {
        taskRepository.deleteTask(task)
    }
}