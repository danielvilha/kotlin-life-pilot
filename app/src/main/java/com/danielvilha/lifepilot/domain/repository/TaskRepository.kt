package com.danielvilha.lifepilot.domain.repository

import com.danielvilha.lifepilot.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>

    suspend fun getTaskById(id: String): Task?

    suspend fun getTaskCount(): Int

    suspend fun insertTask(task: Task)

    suspend fun updateTask(task: Task)

    suspend fun updateTaskOrder(tasks: List<Task>)

    suspend fun deleteTask(task: Task)
}