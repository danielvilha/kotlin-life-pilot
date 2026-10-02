package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.repository.TaskRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class CreateTaskUseCaseTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var createTaskUseCase: CreateTaskUseCase

    @Before
    fun setup() {
        taskRepository = mock()
        createTaskUseCase = CreateTaskUseCase(taskRepository)
    }

    @Test
    fun `should create and insert task with correct values`() = runTest {
        whenever(taskRepository.getTaskCount()).thenReturn(3)

        val parsedTask = ParsedTask(
            title = "Buy milk",
            description = "Buy whole milk",
            dueDate = LocalDate.of(2026, 9, 26),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL
        )

        val result = createTaskUseCase(parsedTask)

        assertNotNull(result.id)
        assertEquals("Buy milk", result.title)
        assertEquals("Buy whole milk", result.description)
        assertEquals(LocalDate.of(2026, 9, 26), result.dueDate)
        assertEquals(Priority.MEDIUM, result.priority)
        assertEquals(TaskCategory.PERSONAL, result.category)
        assertFalse(result.completed)
        assertEquals(3, result.position)

        verify(taskRepository).insertTask(result)
    }
}