package com.danielvilha.lifepilot.feature.ai.data

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskSuggestion

interface AiTaskPlanner {

    suspend fun generateSuggestions(
        tasks: List<Task>
    ): List<TaskSuggestion>
}