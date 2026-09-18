package com.danielvilha.lifepilot.feature.edit.presentation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun TaskEditRoute(
    viewModel: TaskEditViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTaskUpdated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                TaskEditUiEvent.TaskUpdated -> {
                    onTaskUpdated()
                }
            }
        }
    }

    when {
        uiState.isLoading -> {
            CircularProgressIndicator()
        }

        uiState.form != null -> {
            EditTaskScreen(
                form = uiState.form!!,
                isSaving = uiState.isSaving,
                error = uiState.error,
                onCancel = onBack,
                onSave = viewModel::updateTask
            )
        }

        uiState.error != null -> {
            Text(text = uiState.error!!)
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun TaskEditRoutePreview() {
    TaskEditRoute(
        onBack = {},
        onTaskUpdated = {}
    )
}