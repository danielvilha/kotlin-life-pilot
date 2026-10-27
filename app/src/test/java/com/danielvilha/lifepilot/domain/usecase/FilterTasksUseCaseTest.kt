package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTasksUseCaseTest {

    private val useCase = FilterTasksUseCase()

    @Test
    fun `should return all tasks when filter is ALL`() {
        val tasks = listOf(
            createTask(id = "1", completed = true),
            createTask(id = "2", completed = false),
            createTask(id = "3", completed = true)
        )

        val result = useCase(
            tasks = tasks,
            filter = TaskFilter.ALL
        )

        assertEquals(tasks, result)
    }

    @Test
    fun `should return only pending tasks when filter is PENDING`() {
        val tasks = listOf(
            createTask(id = "1", completed = true),
            createTask(id = "2", completed = false),
            createTask(id = "3", completed = true),
            createTask(id = "4", completed = false)
        )

        val result = useCase(
            tasks = tasks,
            filter = TaskFilter.PENDING
        )

        assertEquals(2, result.size)
        assertEquals(
            listOf("2", "4"),
            result.map { it.id }
        )
    }

    @Test
    fun `should return only completed tasks when filter is COMPLETED`() {
        val tasks = listOf(
            createTask(id = "1", completed = true),
            createTask(id = "2", completed = false),
            createTask(id = "3", completed = true),
            createTask(id = "4", completed = false)
        )

        val result = useCase(
            tasks = tasks,
            filter = TaskFilter.COMPLETED
        )

        assertEquals(2, result.size)
        assertEquals(
            listOf("1", "3"),
            result.map { it.id }
        )
    }

    private fun createTask(
        id: String,
        completed: Boolean
    ) = Task(
        id = id,
        title = "Task $id",
        description = null,
        dueDate = null,
        priority = Priority.MEDIUM,
        category = TaskCategory.WORK,
        position = id.toInt(),
        completed = completed
    )
}