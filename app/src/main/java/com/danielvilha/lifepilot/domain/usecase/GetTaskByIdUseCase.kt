package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskByIdUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend operator fun invoke(id: String): Task? {
        return taskRepository.getTaskById(id)
    }
}