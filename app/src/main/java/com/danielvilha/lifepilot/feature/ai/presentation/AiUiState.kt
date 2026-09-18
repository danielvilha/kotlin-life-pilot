package com.danielvilha.lifepilot.feature.ai.presentation

import com.danielvilha.lifepilot.domain.model.ParsedTask

data class AiUiState(
    val input: String = "",
    val isLoading: Boolean = false,
    val parsedTasks: List<ParsedTask> = emptyList(),
    val error: String? = null,
    val editingTaskIndex: Int? = null
)