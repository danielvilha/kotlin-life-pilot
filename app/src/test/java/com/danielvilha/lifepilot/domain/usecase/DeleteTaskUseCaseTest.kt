package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import java.time.LocalDate

class DeleteTaskUseCaseTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase

    @Before
    fun setup() {
        taskRepository = mock()
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository)
    }

    @Test
    fun `should delete task from repository`() = runTest {
        val task = Task(
            id = "1",
            title = "Buy milk",
            description = "Buy whole milk",
            dueDate = LocalDate.of(2026, 9, 26),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL,
            completed = false,
            position = 0
        )

        deleteTaskUseCase(task)

        verify(taskRepository).deleteTask(task)
    }
}