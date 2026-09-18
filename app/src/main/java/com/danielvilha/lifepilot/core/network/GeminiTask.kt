package com.danielvilha.lifepilot.core.network

data class GeminiTask(
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: String,
    val category: String
)
