package com.danielvilha.lifepilot.domain.model

import java.time.LocalDate

data class ParsedTask(
    val title: String,
    val description: String?,
    val dueDate: LocalDate?,
    val priority: Priority,
    val category: TaskCategory
)