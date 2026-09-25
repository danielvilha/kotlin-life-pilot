package com.danielvilha.lifepilot.feature.edit.presentation

data class TaskEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val form: TaskEditForm? = null,
    val error: String? = null,
    val validationError: String? = null
)