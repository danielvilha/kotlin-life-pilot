package com.danielvilha.lifepilot.feature.home.presentation

import com.danielvilha.lifepilot.domain.model.DailyProgress
import com.danielvilha.lifepilot.domain.model.Task

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val isDeleting: Boolean = false,
    val isUpdatingCompletion: Boolean = false
) {
    val dailyProgress: DailyProgress
        get() = DailyProgress(
            totalTasks = tasks.size,
            completedTasks = tasks.count { it.completed }
        )
}