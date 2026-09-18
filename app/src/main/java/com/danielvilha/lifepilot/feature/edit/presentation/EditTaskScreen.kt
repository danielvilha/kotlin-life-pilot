package com.danielvilha.lifepilot.feature.edit.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    form: TaskEditForm,
    isSaving: Boolean = false,
    error: String? = null,
    onCancel: () -> Unit,
    onSave: (TaskEditForm) -> Unit
) {
    var title by rememberSaveable(form.title) {
        mutableStateOf(form.title)
    }

    var description by rememberSaveable(form.description) {
        mutableStateOf(form.description)
    }

    var dueDateText by rememberSaveable(form.dueDate?.toString()) {
        mutableStateOf(form.dueDate?.toString().orEmpty())
    }

    var showDatePicker by rememberSaveable {
        mutableStateOf(false)
    }

    var priority by rememberSaveable(form.priority.name) {
        mutableStateOf(form.priority)
    }

    var category by rememberSaveable(form.category.name) {
        mutableStateOf(form.category)
    }

    var priorityExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var categoryExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var error by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDate = form.dueDate
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.getSelectedDate()?.let { date ->
                            dueDateText = date.toString()
                        }

                        showDatePicker = false
                        error = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            dueDateText = ""
                            showDatePicker = false
                            error = null
                        }
                    ) {
                        Text("Clear")
                    }

                    TextButton(
                        onClick = {
                            showDatePicker = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Edit task")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onCancel
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Cancel"
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

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Title")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "Description")
                }
            )

            OutlinedTextField(
                value = dueDateText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Due date")
                },
                placeholder = {
                    Text("Select a date")
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            showDatePicker = true
                        }
                    ) {
                        Text("Select")
                    }
                }
            )

            ExposedDropdownMenuBox(
                expanded = priorityExpanded,
                onExpandedChange = {
                    priorityExpanded = !priorityExpanded
                }
            ) {
                OutlinedTextField(
                    value = priority.name,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        ),
                    label = {
                        Text(text = "Priority")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = priorityExpanded
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = priorityExpanded,
                    onDismissRequest = {
                        priorityExpanded = false
                    }
                ) {
                    Priority.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(text = option.name)
                            },
                            onClick = {
                                priority = option
                                priorityExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = {
                    categoryExpanded = !categoryExpanded
                }
            ) {
                OutlinedTextField(
                    value = category.name,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                        ),
                    label = {
                        Text(text = "Category")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = categoryExpanded
                        )
                    }
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = {
                        categoryExpanded = false
                    }
                ) {
                    TaskCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(text = option.name)
                            },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            error?.let {
                Text(text = it)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            error = "Title cannot be empty"
                            return@Button
                        }

                        error = null

                        onSave(
                            TaskEditForm(
                                title = title.trim(),
                                description = description,
                                dueDate = dueDateText
                                    .takeIf { it.isNotBlank() }
                                    ?.let(LocalDate::parse),
                                priority = priority,
                                category = category
                            )
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun EditTaskScreenPreview() {
    EditTaskScreen(
        form = TaskEditForm(
            title = "Buy milk",
            description = "Get 2% milk",
            dueDate = LocalDate.now(),
            priority = Priority.MEDIUM,
            category = TaskCategory.SHOPPING
        ),
        isSaving = false,
        error = "Error",
        onCancel = {},
        onSave = {}
    )
}