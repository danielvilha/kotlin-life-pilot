package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class CreateTasksUseCaseTest {

    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var createTasksUseCase: CreateTasksUseCase

    @Before
    fun setup() {
        createTaskUseCase = mock()

        createTasksUseCase = CreateTasksUseCase(
            createTaskUseCase = createTaskUseCase
        )
    }

    @Test
    fun `should create all parsed tasks`() = runTest {
        val parsedTask1 = ParsedTask(
            title = "Buy groceries",
            description = null,
            dueDate = LocalDate.of(2026, 10, 7),
            priority = Priority.MEDIUM,
            category = TaskCategory.SHOPPING
        )

        val parsedTask2 = ParsedTask(
            title = "Finish report",
            description = "Finish Android report",
            dueDate = LocalDate.of(2026, 10, 8),
            priority = Priority.HIGH,
            category = TaskCategory.WORK
        )

        val task1 = Task(
            id = "1",
            title = parsedTask1.title,
            description = parsedTask1.description,
            dueDate = parsedTask1.dueDate,
            priority = parsedTask1.priority,
            category = parsedTask1.category,
            completed = false,
            position = 0
        )

        val task2 = Task(
            id = "2",
            title = parsedTask2.title,
            description = parsedTask2.description,
            dueDate = parsedTask2.dueDate,
            priority = parsedTask2.priority,
            category = parsedTask2.category,
            completed = false,
            position = 1
        )

        whenever(createTaskUseCase(parsedTask1))
            .thenReturn(task1)

        whenever(createTaskUseCase(parsedTask2))
            .thenReturn(task2)

        val result = createTasksUseCase(
            listOf(parsedTask1, parsedTask2)
        )

        assertEquals(
            listOf(task1, task2),
            result
        )

        verify(createTaskUseCase).invoke(parsedTask1)
        verify(createTaskUseCase).invoke(parsedTask2)
    }
}