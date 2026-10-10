package com.danielvilha.lifepilot.feature.plan.presentation

import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskSuggestion

data class PlanMyDayUiState(
    val tasks: List<Task> = emptyList(),
    val suggestions: List<TaskSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val isApplying: Boolean = false,
    val error: String? = null
)

