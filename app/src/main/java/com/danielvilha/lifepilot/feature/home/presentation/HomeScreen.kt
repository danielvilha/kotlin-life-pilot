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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit
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

    HomeScreen(
        uiState = uiState,
        onCreateTask = onCreateTask,
        onEditTask = onEditTask,
        onTasksReordered = viewModel::onTasksReordered,
        onToggleCompleted = viewModel::toggleTaskCompleted,
        onDeleteTask = viewModel::deleteTask,
        snackbarHostState = snackbarHostState
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onCreateTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onTasksReordered: (List<Task>) -> Unit,
    onToggleCompleted: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val lazyListState = rememberLazyListState()

    val tasks = remember { mutableStateListOf<Task>() }
    LaunchedEffect(uiState.tasks) {
        tasks.clear()
        tasks.addAll(uiState.tasks)
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState
    ) { from, to ->
        tasks.add(
            to.index,
            tasks.removeAt(from.index)
        )
    }

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
        if (uiState.tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Today's Tasks")
                Text("No tasks yet.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(
                    modifier = Modifier.height(24.dp)
                )
                Text(
                    text = "Today's Tasks"
                )
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
                                    onTasksReordered(tasks.toList())
                                },
                                onEditClick = {
                                    onEditTask(task.id)
                                },
                                onDeleteClick = onDeleteTask,
                                isDragging = isDragging
                            )
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
    onDeleteClick: (Task) -> Unit,
    isDragging: Boolean
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 2.dp,
        label = "taskCardElevation"
    )

    var showDeleteDialog by rememberSaveable(task.id) {
        mutableStateOf(false)
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
                modifier = with(reorderableScope) {
                    Modifier.draggableHandle(
                        onDragStopped = onDragStopped
                    )
                }
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.title,
                    textDecoration = if (task.completed) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    }
                )

                task.description?.let {
                    Text(text = it)
                }

                Text(text = "Priority: ${task.priority}")

                Text(text = "Category: ${task.category}")

                task.dueDate?.let {
                    Text(text = "Due: $it")
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
                onClick = onEditClick
            ) {
                Text(text = "Edit")
            }

            IconButton(
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
    HomeScreen(
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
        onCreateTask = {},
        onToggleCompleted = {},
        onTasksReordered = {},
        onDeleteTask = {},
        snackbarHostState = SnackbarHostState()
    )
}