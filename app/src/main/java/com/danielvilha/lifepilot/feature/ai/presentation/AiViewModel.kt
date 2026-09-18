package com.danielvilha.lifepilot.feature.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.usecase.CreateTaskUseCase
import com.danielvilha.lifepilot.domain.usecase.ParseTaskUseCase
import com.danielvilha.lifepilot.feature.edit.presentation.TaskEditForm
import com.danielvilha.lifepilot.feature.edit.presentation.toParsedTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class AiViewModel @Inject constructor(
    private val parseTaskUseCase: ParseTaskUseCase,
    private val createTaskUseCase: CreateTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AiUiEvent>()
    val events = _events.asSharedFlow()

    fun onInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(
            input = input
        )
    }

    fun createTask(parsedTask: ParsedTask) {
        viewModelScope.launch {
            try {
                createTaskUseCase(parsedTask)
                _events.emit(AiUiEvent.TaskCreated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create task"
                )
            }
        }
    }

    fun parseTask() {
        val input = _uiState.value.input.trim()

        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                parsedTasks = emptyList()
            )

            try {
                val tasks = parseTaskUseCase(input)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    parsedTasks = tasks
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = mapErrorMessage(e)
                )
            }
        }
    }

    fun startEditingTask(index: Int) {
        if (index !in _uiState.value.parsedTasks.indices) return

        _uiState.value = _uiState.value.copy(
            editingTaskIndex = index,
            error = null
        )
    }

    fun cancelEditingTask() {
        _uiState.value = _uiState.value.copy(
            editingTaskIndex = null,
            error = null
        )
    }

    fun saveEditedTask(form: TaskEditForm) {
        val index = _uiState.value.editingTaskIndex
            ?: return

        val updatedTasks = _uiState.value.parsedTasks.toMutableList()

        if (index !in updatedTasks.indices) return

        updatedTasks[index] = form.toParsedTask()

        _uiState.value = _uiState.value.copy(
            parsedTasks = updatedTasks,
            editingTaskIndex = null,
            error = null
        )
    }

    private fun mapErrorMessage(exception: Exception): String {
        return when (exception) {
            is HttpException -> {
                when (exception.code()) {
                    429 -> "AI is temporarily busy. Please try again in a moment."
                    408 -> "The AI request timed out. Please try again."
                    in 500..599 -> "The AI service is temporarily unavailable."
                    else -> "Unable to analyze your task."
                }
            }

            else -> {
                exception.message ?: "Something went wrong."
            }
        }
    }
}