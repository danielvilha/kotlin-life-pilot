package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Task
import javax.inject.Inject

class CreateTasksUseCase @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase
) {

    suspend operator fun invoke(
        parsedTasks: List<ParsedTask>
    ): List<Task> {
        return parsedTasks.map { parsedTask ->
            createTaskUseCase(parsedTask)
        }
    }
}