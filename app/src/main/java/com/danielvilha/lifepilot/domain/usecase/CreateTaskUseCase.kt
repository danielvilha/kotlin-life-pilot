package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {

    suspend operator fun invoke(parsedTask: ParsedTask): Task {
        val position = taskRepository.getTaskCount()

        val task = Task(
            id = UUID.randomUUID().toString(),
            title = parsedTask.title,
            description = parsedTask.description,
            dueDate = parsedTask.dueDate,
            priority = parsedTask.priority,
            category = parsedTask.category,
            completed = false,
            position = position
        )

        taskRepository.insertTask(task)

        return task
    }
}