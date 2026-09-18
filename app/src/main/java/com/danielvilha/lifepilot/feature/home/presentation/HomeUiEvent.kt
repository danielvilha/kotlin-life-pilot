package com.danielvilha.lifepilot.feature.home.presentation

sealed interface HomeUiEvent {

    data class ShowMessage(
        val message: String
    ) : HomeUiEvent
}