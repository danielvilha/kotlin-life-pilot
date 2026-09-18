package com.danielvilha.lifepilot.feature.edit.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.usecase.GetTaskByIdUseCase
import com.danielvilha.lifepilot.domain.usecase.UpdateTaskUseCase
import com.danielvilha.lifepilot.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel

@OptIn(ExperimentalStdlibApi::class)
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Screen.EditTask>()

    private var originalTask: Task? = null

    private val _uiState = MutableStateFlow(TaskEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskEditUiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            try {
                val task = getTaskByIdUseCase(route.taskId)

                if (task == null) {
                    _uiState.value = TaskEditUiState(
                        isLoading = false,
                        error = "Task not found"
                    )
                    return@launch
                }

                originalTask = task

                _uiState.value = TaskEditUiState(
                    isLoading = false,
                    form = task.toEditForm()
                )

            } catch (e: Exception) {
                _uiState.value = TaskEditUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load task"
                )
            }
        }
    }

    fun updateTask(form: TaskEditForm) {
        val task = originalTask ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null
            )

            try {
                val updatedTask = form.applyTo(task)

                updateTaskUseCase(updatedTask)

                _events.emit(TaskEditUiEvent.TaskUpdated)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to update task"
                )
            }
        }
    }
}