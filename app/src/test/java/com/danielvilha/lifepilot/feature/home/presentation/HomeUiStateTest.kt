package com.danielvilha.lifepilot.feature.home.presentation

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
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

    private fun createTask(
        count: Int,
        completed: Boolean = false
    ): Task {
        return Task(
            id = "$count",
            title = "Task $count",
            description = "Description $count",
            dueDate = LocalDate.of(2026, 9, 26),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL,
            completed = completed,
            position = count
        )
    }
}