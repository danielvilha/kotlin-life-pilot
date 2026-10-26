package com.danielvilha.lifepilot.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyProgressTest {

    @Test
    fun `should calculate 60 percent progress when 3 of 5 tasks are completed`() {
        val progress = DailyProgress(
            totalTasks = 5,
            completedTasks = 3
        )

        val result = progress.percentage

        assertEquals(60, result)
    }

    @Test
    fun `should return zero percent when there are no tasks`() {
        val progress = DailyProgress(
            totalTasks = 0,
            completedTasks = 0
        )

        val result = progress.percentage

        assertEquals(0, result)
    }

    @Test
    fun `should return 100 percent when all tasks are completed`() {
        val progress = DailyProgress(
            totalTasks = 5,
            completedTasks = 5
        )

        val result = progress.percentage

        assertEquals(100, result)
    }
}