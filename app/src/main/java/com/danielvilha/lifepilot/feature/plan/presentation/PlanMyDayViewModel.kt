package com.danielvilha.lifepilot.feature.plan.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielvilha.lifepilot.domain.usecase.GenerateTaskSuggestionsUseCase
import com.danielvilha.lifepilot.domain.usecase.ObserveTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.ReorderTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanMyDayViewModel @Inject constructor(
    private val observeTasksUseCase: ObserveTasksUseCase,
    private val generateTaskSuggestionsUseCase: GenerateTaskSuggestionsUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlanMyDayUiState()
    )

    val uiState: StateFlow<PlanMyDayUiState> =
        _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PlanMyDayUiEvent>()
    val uiEvent: SharedFlow<PlanMyDayUiEvent> = _uiEvent.asSharedFlow()

    init {
        observeTasks()
    }

    fun generatePlan() {
        val tasks = _uiState.value.tasks

        if (tasks.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                suggestions = emptyList()
            )

            try {
                val suggestions = generateTaskSuggestionsUseCase(tasks)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    suggestions = suggestions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to generate your plan."
                )
            }
        }
    }

    fun applyPlan() {
        val currentState = _uiState.value

        if (
            currentState.isApplying ||
            currentState.isLoading ||
            currentState.suggestions.isEmpty()
        ) return

        val tasksById = currentState.tasks.associateBy { it.id }

        val pendingTasks = currentState.tasks.filterNot { it.completed }

        val orderedSuggestions = currentState.suggestions
            .sortedBy { it.suggestedOrder }

        val suggestedIds = orderedSuggestions.map { it.taskId }

        if (
            suggestedIds.size != pendingTasks.size ||
            suggestedIds.toSet().size != suggestedIds.size ||
            suggestedIds.toSet() != pendingTasks.map { it.id }.toSet()
        ) {
            _uiState.value = currentState.copy(
                error = "The suggested plan is incomplete. Please generate it again."
            )
            return
        }

        val orderedPendingTasks = suggestedIds.mapNotNull { id ->
            tasksById[id]
        }

        val completedTasks = currentState.tasks.filter { it.completed }

        val reorderedTasks = (
                orderedPendingTasks + completedTasks
                ).mapIndexed { index, task ->
                task.copy(position = index)
            }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isApplying = true,
                error = null
            )

            try {
                reorderTasksUseCase(reorderedTasks)

                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    suggestions = emptyList()
                )

                _uiEvent.emit(PlanMyDayUiEvent.PlanApplied)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isApplying = false,
                    error = e.message ?: "Failed to apply your plan."
                )
            }
        }
    }

    private fun observeTasks() {
        viewModelScope.launch {
            observeTasksUseCase().collect { tasks ->
                _uiState.value = _uiState.value.copy(
                    tasks = tasks
                )
            }
        }
    }
}