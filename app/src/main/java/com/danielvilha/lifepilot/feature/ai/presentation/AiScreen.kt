package com.danielvilha.lifepilot.feature.ai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.feature.edit.presentation.EditTaskScreen
import com.danielvilha.lifepilot.feature.edit.presentation.toEditForm

@Composable
fun AiScreen(
    viewModel: AiViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onTaskCreated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AiUiEvent.TasksCreated -> onTaskCreated()
            }
        }
    }

    val editingIndex = uiState.editingTaskIndex

    if (editingIndex != null && editingIndex in uiState.parsedTasks.indices) {
        val editingForm = uiState.parsedTasks[editingIndex].toEditForm()

        EditTaskScreen(
            form = editingForm,
            onCancel = viewModel::cancelEditingTask,
            onSave = viewModel::saveEditedTask
        )
    } else {
        AiScreen(
            uiState = uiState,
            onBack = onBack,
            onInputChanged = viewModel::onInputChanged,
            onParseTask = viewModel::parseTask,
            onCreateClick = viewModel::createTasks,
            onEditClick = viewModel::startEditingTask,
            onRemoveClick = viewModel::removeTask
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    uiState: AiUiState,
    onBack: () -> Unit = {},
    onInputChanged: (String) -> Unit,
    onParseTask: () -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = { Text("Create with AI") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(text = "What do you need to get done?")

            OutlinedTextField(
                value = uiState.input,
                onValueChange = onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = "e.g. Buy milk tomorrow")
                }
            )

            Button(
                onClick = onParseTask,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.input.isNotBlank() && !uiState.isLoading
            ) {
                Text(text = "Analyze")
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            uiState.error?.let { error ->
                Text(text = error)
            }

            if (uiState.parsedTasks.isNotEmpty()) {
                Text(
                    text = "AI found ${uiState.parsedTasks.size} task(s)"
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = uiState.parsedTasks
                    ) { index, task ->

                        ParsedTaskCard(
                            task = task,
                            onEditClick = {
                                onEditClick(index)
                            },
                            onRemoveClick = {
                                onRemoveClick(index)
                            }
                        )
                    }
                }

                Button(
                    onClick = onCreateClick,
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = if (uiState.parsedTasks.size == 1) {
                                "Create task"
                            } else {
                                "Create ${uiState.parsedTasks.size} tasks"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedTaskCard(
    task: ParsedTask,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(
                    onClick = onEditClick
                ) {
                    Text(text = "Edit")
                }

                TextButton(
                    onClick = onRemoveClick
                ) {
                    Text(text = "Remove")
                }
            }

            task.description
                ?.takeIf { it.isNotBlank() }
                ?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

            Text(
                text = buildString {
                    append(task.priority.name)
                    append("  •  ")
                    append(task.category.name)

                    task.dueDate?.let { dueDate ->
                        append("  •  ")
                        append(dueDate)
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AiScreenPreview() {
    AiScreen(
        uiState = AiUiState(
            input = "Buy milk tomorrow",
            isLoading = false,
            parsedTasks = listOf(
                ParsedTask(
                    title = "Buy milk",
                    description = "Get 2% milk",
                    dueDate = null,
                    priority = Priority.MEDIUM,
                    category = TaskCategory.SHOPPING
                )
            )
        ),
        onBack = {},
        onInputChanged = {},
        onParseTask = {},
        onCreateClick = {},
        onEditClick = {},
        onRemoveClick = {}
    )
}