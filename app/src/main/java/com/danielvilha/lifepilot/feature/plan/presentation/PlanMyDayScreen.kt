package com.danielvilha.lifepilot.feature.plan.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory

@Composable
fun PlanMyDayScreen(
    onBackClick: () -> Unit,
    onPlanApplied: () -> Unit,
    viewModel: PlanMyDayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                PlanMyDayUiEvent.PlanApplied -> {
                    onPlanApplied()
                }
            }
        }
    }

    PlanMyDayContent(
        uiState = uiState,
        onGeneratePlanClick = viewModel::generatePlan,
        onApplyPlanClick = viewModel::applyPlan,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanMyDayContent(
    uiState: PlanMyDayUiState,
    onGeneratePlanClick: () -> Unit,
    onApplyPlanClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val plannedTasks = remember(
        uiState.tasks,
        uiState.suggestions
    ) {
        val tasksById = uiState.tasks.associateBy { task ->
            task.id
        }

        uiState.suggestions
            .sortedBy { suggestion ->
                suggestion.suggestedOrder
            }
            .mapNotNull { suggestion ->
                tasksById[suggestion.taskId]?.let { task ->
                    task to suggestion
                }
            }
    }

    val hasPendingTasks = uiState.tasks.any { task ->
        !task.completed
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = { Text(text = "Plan My Day") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Let AI suggest the best order for your pending tasks.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onGeneratePlanClick,
                enabled = hasPendingTasks &&
                        !uiState.isLoading &&
                        !uiState.isApplying,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.suggestions.isEmpty()) {
                        "Generate Plan"
                    } else {
                        "Regenerate Plan"
                    }
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )

                Text(
                    text = "AI is planning your day...",
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!hasPendingTasks && !uiState.isLoading) {
                Text(
                    text = "You don't have any pending tasks to plan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (plannedTasks.isNotEmpty() && !uiState.isLoading) {
                HorizontalDivider()

                Text(
                    text = "Your AI Plan",
                    style = MaterialTheme.typography.titleLarge
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        bottom = 24.dp
                    )
                ) {
                    items(
                        items = plannedTasks,
                        key = { (task, _) -> task.id }
                    ) { (task, suggestion) ->

                        PlanSuggestionCard(
                            task = task,
                            suggestion = suggestion
                        )
                    }
                }

                Button(
                    onClick = onApplyPlanClick,
                    enabled = !uiState.isApplying &&
                            !uiState.isLoading &&
                            plannedTasks.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Applying Plan...")
                    } else {
                        Text("Apply This Plan")
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PlanMyDayScreenPreview() {
    PlanMyDayContent(
        uiState = PlanMyDayUiState(
            tasks = listOf(
                Task(
                    id = "1",
                    title = "Complete project documentation",
                    description = "Write the final report for LifePilot",
                    dueDate = null,
                    priority = Priority.HIGH,
                    category = TaskCategory.WORK,
                    position = 1
                )
            )
        ),
        onGeneratePlanClick = {},
        onApplyPlanClick = {},
        onBackClick = {}
    )
}