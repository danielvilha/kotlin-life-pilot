package com.danielvilha.lifepilot.feature.plan.presentation

sealed interface PlanMyDayUiEvent {
    data object PlanApplied : PlanMyDayUiEvent
}