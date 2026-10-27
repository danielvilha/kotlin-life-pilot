package com.danielvilha.lifepilot.feature.home.presentation

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskFilter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeUiStateTest {

    @Test
    fun `should calculate daily progress from tasks`() {
        val tasks = listOf(
            createTask(1, completed = true),
            createTask(2, completed = true),
            createTask(3, completed = true),
            createTask(4, completed = false),
            createTask(5, completed = false)
        )

        val uiState = HomeUiState(tasks = tasks)

        assertEquals(5, uiState.dailyProgress.totalTasks)
        assertEquals(3, uiState.dailyProgress.completedTasks)
        assertEquals(60, uiState.dailyProgress.percentage)
    }

    @Test
    fun `should preserve filtered tasks without affecting daily progress`() {
        val tasks = listOf(
            createTask(id = 1, completed = true),
            createTask(id = 2, completed = false),
            createTask(id = 3, completed = true),
            createTask(id = 4, completed = false)
        )

        val filteredTasks = listOf(tasks[1], tasks[3])

        val uiState = HomeUiState(
            tasks = tasks,
            selectedFilter = TaskFilter.PENDING,
            filteredTasks = filteredTasks
        )

        assertEquals(TaskFilter.PENDING, uiState.selectedFilter)

        assertEquals(
            listOf("2", "4"),
            uiState.filteredTasks.map { it.id }
        )

        assertEquals(4, uiState.dailyProgress.totalTasks)
        assertEquals(2, uiState.dailyProgress.completedTasks)
        assertEquals(50, uiState.dailyProgress.percentage)
    }

    private fun createTask(
        id: Int,
        completed: Boolean = false
    ): Task {
        return Task(
            id = "$id",
            title = "Task $id",
            description = "Description $id",
            dueDate = LocalDate.of(2026, 9, 26),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL,
            completed = completed,
            position = id
        )
    }
}