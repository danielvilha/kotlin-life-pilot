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

        val suggestionTaskIds = suggestions.map { suggestion ->
            suggestion.taskId
        }

        if (suggestionTaskIds.size != suggestionTaskIds.distinct().size) {
            throw IllegalStateException(
                "AI plan contains duplicate task IDs"
            )
        }

        val validTaskIds = pendingTasks
            .map { task -> task.id }
            .toSet()

        val unknownTaskIds = suggestionTaskIds.toSet() - validTaskIds

        if (unknownTaskIds.isNotEmpty()) {
            throw IllegalStateException(
                "AI plan contains unknown task IDs"
            )
        }

        val missingTaskIds = validTaskIds - suggestionTaskIds.toSet()

        if (missingTaskIds.isNotEmpty()) {
            throw IllegalStateException(
                "AI plan is missing pending tasks"
            )
        }

        val suggestedOrders = suggestions.map { suggestion ->
            suggestion.suggestedOrder
        }

        if (suggestedOrders.size != suggestedOrders.distinct().size) {
            throw IllegalStateException(
                "AI plan contains duplicate suggested orders"
            )
        }

        val sortedOrders = suggestedOrders.sorted()

        val expectedOrders = suggestions.indices.toList()

        if (sortedOrders != expectedOrders) {
            throw IllegalStateException(
                "AI plan contains non-consecutive suggested orders"
            )
        }

        return suggestions.sortedBy { suggestion ->
            suggestion.suggestedOrder
        }
    }
}