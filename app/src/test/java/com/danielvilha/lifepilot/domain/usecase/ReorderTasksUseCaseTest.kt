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

class ReorderTasksUseCaseTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var reorderTasksUseCase: ReorderTasksUseCase

    @Before
    fun setup() {
        taskRepository = mock()
        reorderTasksUseCase = ReorderTasksUseCase(taskRepository)
    }

    @Test
    fun `should update task positions according to their order`() = runTest {
        val firstTask = Task(
            id = "1",
            title = "Buy milk",
            description = "Buy whole milk",
            dueDate = LocalDate.of(2026, 9, 26),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL,
            completed = false,
            position = 5
        )

        val secondTask = Task(
            id = "2",
            title = "Call John",
            description = "Discuss the project",
            dueDate = LocalDate.of(2026, 9, 27),
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 2
        )

        val thirdTask = Task(
            id = "3",
            title = "Go running",
            description = null,
            dueDate = LocalDate.of(2026, 9, 28),
            priority = Priority.LOW,
            category = TaskCategory.PERSONAL,
            completed = false,
            position = 8
        )

        val tasks = listOf(
            firstTask,
            secondTask,
            thirdTask
        )

        reorderTasksUseCase(tasks)

        verify(taskRepository).updateTaskOrder(
            listOf(
                firstTask.copy(position = 0),
                secondTask.copy(position = 1),
                thirdTask.copy(position = 2)
            )
        )
    }
}