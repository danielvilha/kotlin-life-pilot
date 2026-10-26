package com.danielvilha.lifepilot.domain.model

data class DailyProgress(
    val totalTasks: Int,
    val completedTasks: Int
) {
    val percentage: Int
        get() = if (totalTasks > 0) {
            (completedTasks * 100) / totalTasks
        } else {
            0
        }
}
