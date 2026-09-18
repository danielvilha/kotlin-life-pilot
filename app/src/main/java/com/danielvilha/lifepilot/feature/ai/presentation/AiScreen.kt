package com.danielvilha.lifepilot.feature.ai.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                AiUiEvent.TaskCreated -> onTaskCreated()
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
            onCreateClick = viewModel::createTask,
            onEditClick = viewModel::startEditingTask
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
    onCreateClick: (ParsedTask) -> Unit,
    onEditClick: (Int) -> Unit,
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
                Text(text = "AI found ${uiState.parsedTasks.size} task(s)")

                uiState.parsedTasks.forEachIndexed { index, task ->
                    ParsedTaskCard(
                        task = task,
                        onEditClick = {
                            onEditClick(index)
                        },
                        onCreateClick = onCreateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedTaskCard(
    task: ParsedTask,
    onCreateClick: (ParsedTask) -> Unit,
    onEditClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = task.title)

            task.description?.let {
                Text(text = it)
            }

            HorizontalDivider()

            Text(text = "Priority: ${task.priority}")

            Text(text = "Category: ${task.category}")

            task.dueDate?.let {
                Text(text = "Due: $it")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onEditClick() }
                ) {
                    Text(text = "Edit")
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { onCreateClick(task) }
                ) {
                    Text(text = "Create")
                }
            }
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
        onEditClick = {}
    )
}