package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.feature.ai.data.AiTaskPlanner
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskSuggestion
import javax.inject.Inject

class GenerateTaskSuggestionsUseCase @Inject constructor(
    private val aiTaskPlanner: AiTaskPlanner
) {

    suspend operator fun invoke(
        tasks: List<Task>
    ): List<TaskSuggestion> {

        val pendingTasks = tasks.filterNot { task ->
            task.completed
        }

        if (pendingTasks.isEmpty()) {
            return emptyList()
        }

        val suggestions = aiTaskPlanner.generateSuggestions(
            pendingTasks
        )

        val validTaskIds = pendingTasks
            .map { task -> task.id }
            .toSet()

        return suggestions
            .filter { suggestion ->
                suggestion.taskId in validTaskIds
            }
            .sortedBy { suggestion ->
                suggestion.suggestedOrder
            }
    }
}