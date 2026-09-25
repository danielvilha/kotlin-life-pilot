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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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

    private val _isDeleting = MutableStateFlow(false)
    private val _isUpdatingCompletion = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
        combine(
            observeTasksUseCase(),
            _isDeleting,
            _isUpdatingCompletion
        ) { tasks, isDeleting, isUpdatingCompletion ->
            HomeUiState(
                tasks = tasks,
                isDeleting = isDeleting,
                isUpdatingCompletion = isUpdatingCompletion
            )
        }.stateIn(
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
            _isUpdatingCompletion.value = true

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
            } finally {
                _isUpdatingCompletion.value = false
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            _isDeleting.value = true

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
            } finally {
                _isDeleting.value = false
            }
        }
    }
}