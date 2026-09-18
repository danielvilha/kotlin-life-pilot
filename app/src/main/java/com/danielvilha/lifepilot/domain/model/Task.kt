package com.danielvilha.lifepilot.domain.model

import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val dueDate: LocalDate?,
    val priority: Priority,
    val category: TaskCategory,
    val completed: Boolean = false,
    val position: Int
)