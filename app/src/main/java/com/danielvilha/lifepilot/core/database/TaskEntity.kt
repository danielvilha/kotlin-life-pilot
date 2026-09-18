package com.danielvilha.lifepilot.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val priority: String,
    val category: String,
    val completed: Boolean,
    val position: Int
)