package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskFilter
import javax.inject.Inject

class FilterTasksUseCase @Inject constructor() {

    operator fun invoke(
        tasks: List<Task>,
        filter: TaskFilter
    ): List<Task> {
        return when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.PENDING -> tasks.filter { !it.completed }
            TaskFilter.COMPLETED -> tasks.filter { it.completed }
        }
    }
}