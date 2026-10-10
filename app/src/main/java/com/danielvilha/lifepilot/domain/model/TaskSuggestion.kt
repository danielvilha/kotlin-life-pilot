package com.danielvilha.lifepilot.domain.model

data class TaskSuggestion(
    val taskId: String,
    val reason: String,
    val suggestedOrder: Int
)
