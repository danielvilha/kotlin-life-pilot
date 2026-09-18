package com.danielvilha.lifepilot.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {

    @Serializable
    data object Home : Screen

    @Serializable
    data object Ai : Screen

    @Serializable
    data class EditTask(
        val taskId: String
    ) : Screen
}