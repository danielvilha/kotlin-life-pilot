package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.feature.ai.data.AiTaskPlanner
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskSuggestion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertFailsWith

class GenerateTaskSuggestionsUseCaseTest {

    private lateinit var aiTaskPlanner: AiTaskPlanner
    private lateinit var useCase: GenerateTaskSuggestionsUseCase

    @Before
    fun setup() {
        aiTaskPlanner = mock()
        useCase = GenerateTaskSuggestionsUseCase(aiTaskPlanner)
    }

    @Test
    fun `should generate suggestions for pending tasks`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Prepare interview",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                completed = false,
                position = 0
            ),
            Task(
                id = "task-2",
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING,
                completed = false,
                position = 1
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "High priority and due today.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-2",
                reason = "Can be completed after the interview preparation.",
                suggestedOrder = 1
            )
        )

        whenever(
            aiTaskPlanner.generateSuggestions(tasks)
        ).thenReturn(suggestions)

        val result = useCase(tasks)

        assertEquals(
            suggestions,
            result
        )

        verify(aiTaskPlanner).generateSuggestions(tasks)
    }

    @Test
    fun `should send only pending tasks to AI planner`() = runTest {
        val pendingTask = Task(
            id = "task-1",
            title = "Prepare interview",
            description = null,
            dueDate = LocalDate.of(2026, 10, 7),
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )

        val completedTask = Task(
            id = "task-2",
            title = "Buy groceries",
            description = null,
            dueDate = LocalDate.of(2026, 10, 6),
            priority = Priority.MEDIUM,
            category = TaskCategory.SHOPPING,
            completed = true,
            position = 1
        )

        val pendingTasks = listOf(pendingTask)

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "High priority and due soon.",
                suggestedOrder = 0
            )
        )

        whenever(
            aiTaskPlanner.generateSuggestions(pendingTasks)
        ).thenReturn(suggestions)

        val result = useCase(
            listOf(
                pendingTask,
                completedTask
            )
        )

        assertEquals(
            suggestions,
            result
        )

        verify(aiTaskPlanner).generateSuggestions(pendingTasks)
    }

    @Test
    fun `should not call AI planner when all tasks are completed`() = runTest {
        val completedTasks = listOf(
            Task(
                id = "task-1",
                title = "Prepare interview",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                completed = true,
                position = 0
            ),
            Task(
                id = "task-2",
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 6),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING,
                completed = true,
                position = 1
            )
        )

        val result = useCase(completedTasks)

        assertTrue(result.isEmpty())

        verify(
            aiTaskPlanner,
            never()
        ).generateSuggestions(any())
    }

    @Test
    fun `should reject suggestions with unknown task id`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Prepare interview",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                completed = false,
                position = 0
            ),
            Task(
                id = "task-2",
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING,
                completed = false,
                position = 1
            )
        )

        val validSuggestion1 = TaskSuggestion(
            taskId = "task-1",
            reason = "High priority and due soon.",
            suggestedOrder = 0
        )

        val invalidSuggestion = TaskSuggestion(
            taskId = "task-999",
            reason = "This task does not exist.",
            suggestedOrder = 1
        )

        val validSuggestion2 = TaskSuggestion(
            taskId = "task-2",
            reason = "Can be completed afterwards.",
            suggestedOrder = 2
        )

        whenever(
            aiTaskPlanner.generateSuggestions(tasks)
        ).thenReturn(
            listOf(
                validSuggestion1,
                invalidSuggestion,
                validSuggestion2
            )
        )

        val exception = assertFailsWith<IllegalStateException> {
            useCase(tasks)
        }

        assertEquals(
            "AI plan contains unknown task IDs",
            exception.message
        )
    }

    @Test
    fun `should sort suggestions by suggested order`() = runTest {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Prepare interview",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                completed = false,
                position = 0
            ),
            Task(
                id = "task-2",
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING,
                completed = false,
                position = 1
            ),
            Task(
                id = "task-3",
                title = "Call the dentist",
                description = null,
                dueDate = LocalDate.of(2026, 10, 9),
                priority = Priority.LOW,
                category = TaskCategory.HEALTH,
                completed = false,
                position = 2
            )
        )

        val firstSuggestion = TaskSuggestion(
            taskId = "task-1",
            reason = "Highest priority.",
            suggestedOrder = 0
        )

        val secondSuggestion = TaskSuggestion(
            taskId = "task-3",
            reason = "Should be handled next.",
            suggestedOrder = 1
        )

        val thirdSuggestion = TaskSuggestion(
            taskId = "task-2",
            reason = "Can be completed later.",
            suggestedOrder = 2
        )

        whenever(
            aiTaskPlanner.generateSuggestions(tasks)
        ).thenReturn(
            listOf(
                thirdSuggestion,
                firstSuggestion,
                secondSuggestion
            )
        )

        val result = useCase(tasks)

        assertEquals(
            listOf(
                firstSuggestion,
                secondSuggestion,
                thirdSuggestion
            ),
            result
        )
    }

    @Test
    fun `should reject suggestions with duplicate task IDs`() = runTest {
        val tasks = listOf(
            createTask(id = "task-1"),
            createTask(id = "task-2")
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "High priority",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Important task",
                suggestedOrder = 1
            )
        )

        whenever(
            aiTaskPlanner.generateSuggestions(any())
        ).thenReturn(suggestions)

        val exception = assertFailsWith<IllegalStateException> {
            useCase(tasks)
        }

        assertEquals(
            "AI plan contains duplicate task IDs",
            exception.message
        )
    }

    @Test
    fun `should reject suggestions when a pending task is missing`() = runTest {
        val tasks = listOf(
            createTask(id = "task-1"),
            createTask(id = "task-2"),
            createTask(id = "task-3")
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "High priority",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-3",
                reason = "Can be completed next",
                suggestedOrder = 1
            )
        )

        whenever(
            aiTaskPlanner.generateSuggestions(any())
        ).thenReturn(suggestions)

        val exception = assertFailsWith<IllegalStateException> {
            useCase(tasks)
        }

        assertEquals(
            "AI plan is missing pending tasks",
            exception.message
        )
    }

    @Test
    fun `should reject suggestions with duplicate suggested orders`() = runTest {
        val tasks = listOf(
            createTask(id = "task-1"),
            createTask(id = "task-2")
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "High priority",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-2",
                reason = "Due soon",
                suggestedOrder = 0
            )
        )

        whenever(
            aiTaskPlanner.generateSuggestions(any())
        ).thenReturn(suggestions)

        val exception = assertFailsWith<IllegalStateException> {
            useCase(tasks)
        }

        assertEquals(
            "AI plan contains duplicate suggested orders",
            exception.message
        )
    }

    @Test
    fun `should reject suggestions with non consecutive orders`() = runTest {
        val tasks = listOf(
            createTask(id = "task-1"),
            createTask(id = "task-2"),
            createTask(id = "task-3")
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "High priority",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-2",
                reason = "Due soon",
                suggestedOrder = 2
            ),
            TaskSuggestion(
                taskId = "task-3",
                reason = "Can be completed later",
                suggestedOrder = 4
            )
        )

        whenever(
            aiTaskPlanner.generateSuggestions(any())
        ).thenReturn(suggestions)

        val exception = assertFailsWith<IllegalStateException> {
            useCase(tasks)
        }

        assertEquals(
            "AI plan contains non-consecutive suggested orders",
            exception.message
        )
    }

    private fun createTask(
        id: String
    ): Task {
        return Task(
            id = id,
            title = "Generated task $id",
            description = null,
            dueDate = LocalDate.now(),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL,
            completed = false,
            position = 0
        )
    }
}