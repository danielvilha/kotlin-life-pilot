package com.danielvilha.lifepilot.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.usecase.DeleteTaskUseCase
import com.danielvilha.lifepilot.domain.usecase.ObserveTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.ReorderTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.UpdateTaskCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeTasksUseCase: ObserveTasksUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val updateTaskCompletionUseCase: UpdateTaskCompletionUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<HomeUiEvent>()

    val events = _events.asSharedFlow()

    val uiState: StateFlow<HomeUiState> =
        observeTasksUseCase()
            .map { tasks -> HomeUiState(tasks = tasks) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState()
            )

    fun onTasksReordered(tasks: List<Task>) {
        viewModelScope.launch {
            reorderTasksUseCase(tasks)
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            try {
                updateTaskCompletionUseCase(
                    task = task,
                    completed = !task.completed
                )
            } catch (e: Exception) {
                _events.emit(
                    HomeUiEvent.ShowMessage(
                        e.message ?: "Failed to update task"
                    )
                )
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                deleteTaskUseCase(task)

                _events.emit(
                    HomeUiEvent.ShowMessage("Task deleted")
                )

            } catch (e: Exception) {
                _events.emit(
                    HomeUiEvent.ShowMessage(
                        e.message ?: "Failed to delete task"
                    )
                )
            }
        }
    }
}