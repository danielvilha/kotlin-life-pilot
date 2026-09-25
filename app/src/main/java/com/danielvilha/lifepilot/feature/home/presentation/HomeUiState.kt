package com.danielvilha.lifepilot.feature.home.presentation

import com.danielvilha.lifepilot.domain.model.Task

data class HomeUiState(
    val tasks: List<Task> = emptyList(),
    val isDeleting: Boolean = false
)