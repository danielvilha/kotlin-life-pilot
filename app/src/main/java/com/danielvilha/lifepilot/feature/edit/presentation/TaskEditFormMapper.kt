package com.danielvilha.lifepilot.feature.edit.presentation

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Task

fun ParsedTask.toEditForm(): TaskEditForm {
    return TaskEditForm(
        title = title,
        description = description.orEmpty(),
        dueDate = dueDate,
        priority = priority,
        category = category
    )
}

fun Task.toEditForm(): TaskEditForm {
    return TaskEditForm(
        title = title,
        description = description.orEmpty(),
        dueDate = dueDate,
        priority = priority,
        category = category
    )
}

fun TaskEditForm.toParsedTask(): ParsedTask {
    return ParsedTask(
        title = title.trim(),
        description = description.ifBlank { null },
        dueDate = dueDate,
        priority = priority,
        category = category
    )
}

fun TaskEditForm.applyTo(task: Task): Task {
    return task.copy(
        title = title.trim(),
        description = description.ifBlank { null },
        dueDate = dueDate,
        priority = priority,
        category = category
    )
}