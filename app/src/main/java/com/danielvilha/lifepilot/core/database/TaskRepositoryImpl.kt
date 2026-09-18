package com.danielvilha.lifepilot.core.database

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> {
        return taskDao.observeTasks()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun getTaskCount(): Int {
        return taskDao.getTaskCount()
    }

    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun updateTaskOrder(tasks: List<Task>) {
        taskDao.updateTaskOrder(
            tasks.map { it.toEntity() }
        )
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
}

private fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate?.let(LocalDate::parse),
        priority = Priority.valueOf(priority),
        category = TaskCategory.valueOf(category),
        completed = completed,
        position = position
    )
}

private fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate?.toString(),
        priority = priority.name,
        category = category.name,
        completed = completed,
        position = position
    )
}