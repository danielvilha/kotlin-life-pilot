package com.danielvilha.lifepilot.feature.home.presentation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskFilter
import com.danielvilha.lifepilot.feature.home.presentation.components.DailyProgressCard
import com.danielvilha.lifepilot.feature.home.presentation.components.TaskFilterBar
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onPlanMyDay: () -> Unit,
    planApplied: Boolean = false,
    onPlanAppliedConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        event.message
                    )
                }
            }
        }
    }

    LaunchedEffect(planApplied) {
        if (planApplied) {
            viewModel.showPlanAppliedMessage()
            onPlanAppliedConsumed()
        }
    }

    HomeContent(
        uiState = uiState,
        onCreateTask = onCreateTask,
        onEditTask = onEditTask,
        onPlanMyDay = onPlanMyDay,
        onTasksReordered = viewModel::onTasksReordered,
        onToggleCompleted = viewModel::toggleTaskCompleted,
        onDeleteTask = viewModel::deleteTask,
        onFilterSelected = viewModel::selectFilter,
        snackbarHostState = snackbarHostState
    )
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onPlanMyDay: () -> Unit,
    onTasksReordered: (List<Task>) -> Unit,
    onToggleCompleted: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onFilterSelected: (TaskFilter) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTask
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create task"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Today's Tasks",
                style = MaterialTheme.typography.headlineSmall
            )

            DailyProgressCard(
                progress = uiState.dailyProgress
            )

            TaskFilterBar(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onFilterSelected
            )

            if (uiState.tasks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .width(72.dp)
                            .height(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Your day starts here!",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Create your first task and start making progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

            } else {
                Button(
                    onClick = onPlanMyDay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Plan My Day")
                }

                key(uiState.selectedFilter) {
                    val lazyListState = rememberLazyListState()

                    val tasks = remember(uiState.filteredTasks) {
                        uiState.filteredTasks.toMutableStateList()
                    }

                    val reorderableState = rememberReorderableLazyListState(
                        lazyListState
                    ) { from, to ->
                        tasks.add(
                            to.index,
                            tasks.removeAt(from.index)
                        )
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            items = tasks,
                            key = { task -> task.id }
                        ) { task ->
                            ReorderableItem(
                                state = reorderableState,
                                key = task.id
                            ) { isDragging ->
                                TaskCard(
                                    task = task,
                                    reorderableScope = this,
                                    onDragStopped = {
                                        if (uiState.selectedFilter == TaskFilter.ALL) {
                                            onTasksReordered(tasks.toList())
                                        }
                                    },
                                    onEditClick = {
                                        onEditTask(task.id)
                                    },
                                    onToggleCompleted = onToggleCompleted,
                                    onDeleteClick = onDeleteTask,
                                    isDragging = isDragging,
                                    isDeleting = uiState.isDeleting,
                                    isReorderEnabled = uiState.selectedFilter == TaskFilter.ALL,
                                    isUpdatingCompletion = uiState.isUpdatingCompletion
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    reorderableScope: ReorderableCollectionItemScope,
    onDragStopped: () -> Unit,
    onEditClick: () -> Unit,
    onToggleCompleted: (Task) -> Unit,
    onDeleteClick: (Task) -> Unit,
    isDragging: Boolean,
    isDeleting: Boolean,
    isReorderEnabled: Boolean,
    isUpdatingCompletion: Boolean
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        label = "taskCardElevation"
    )

    var showDeleteDialog by rememberSaveable(task.id) {
        mutableStateOf(false)
    }

    val contentColor = if (task.completed) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val textDecoration = if (task.completed) {
        TextDecoration.LineThrough
    } else {
        TextDecoration.None
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(text = "Delete task?")
            },
            text = {
                Text(text = "Are you sure you want to delete \"${task.title}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick(task)
                    }
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reorder task",
                modifier = if (isReorderEnabled) {
                    with(reorderableScope) {
                        Modifier.draggableHandle(
                            onDragStopped = onDragStopped
                        )
                    }
                } else {
                    Modifier
                },
                tint = if (isReorderEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Checkbox(
                checked = task.completed,
                enabled = !isDeleting && !isUpdatingCompletion,
                onCheckedChange = {
                    onToggleCompleted(task)
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    textDecoration = textDecoration
                )

                task.description?.let {
                    Text(
                        text = it,
                        color = contentColor,
                        textDecoration = textDecoration
                    )
                }

                Text(
                    text = "Priority: ${task.priority}",
                    textDecoration = textDecoration
                )

                Text(
                    text = "Category: ${task.category}",
                    textDecoration = textDecoration
                )

                task.dueDate?.let {
                    Text(
                        text = "Due: $it",
                        textDecoration = textDecoration
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = !isDeleting&& !isUpdatingCompletion,
                onClick = onEditClick
            ) {
                Text(text = "Edit")
            }

            IconButton(
                enabled = !isDeleting&& !isUpdatingCompletion,
                onClick = {
                    showDeleteDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete task"
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeContent(
        uiState = HomeUiState(
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
        onEditTask = {},
        onPlanMyDay = {},
        onCreateTask = {},
        onToggleCompleted = {},
        onTasksReordered = {},
        onDeleteTask = {},
        onFilterSelected = {},
        snackbarHostState = SnackbarHostState()
    )
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenEmptyPreview() {
    HomeContent(
        uiState = HomeUiState(
            tasks = listOf()
        ),
        onEditTask = {},
        onPlanMyDay = {},
        onCreateTask = {},
        onToggleCompleted = {},
        onTasksReordered = {},
        onDeleteTask = {},
        onFilterSelected = {},
        snackbarHostState = SnackbarHostState()
    )
}