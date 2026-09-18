package com.danielvilha.lifepilot.feature.ai.presentation

sealed interface AiUiEvent {

    data object TaskCreated : AiUiEvent
}