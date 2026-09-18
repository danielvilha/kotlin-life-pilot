package com.danielvilha.lifepilot.feature.edit.presentation

sealed interface TaskEditUiEvent {

    data object TaskUpdated : TaskEditUiEvent
}