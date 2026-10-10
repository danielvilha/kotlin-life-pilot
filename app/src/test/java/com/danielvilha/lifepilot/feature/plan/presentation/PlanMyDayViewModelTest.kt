package com.danielvilha.lifepilot.feature.plan.presentation

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskSuggestion
import com.danielvilha.lifepilot.domain.usecase.GenerateTaskSuggestionsUseCase
import com.danielvilha.lifepilot.domain.usecase.ObserveTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.ReorderTasksUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class PlanMyDayViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var observeTasksUseCase: ObserveTasksUseCase
    private lateinit var generateTaskSuggestionsUseCase: GenerateTaskSuggestionsUseCase
    private lateinit var reorderTasksUseCase: ReorderTasksUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        observeTasksUseCase = mock()
        generateTaskSuggestionsUseCase = mock()
        reorderTasksUseCase = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should expose observed tasks`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            ),
            createTask(
                id = "task-2",
                title = "Buy groceries"
            )
        )

        val tasksFlow = MutableStateFlow(tasks)

        whenever(observeTasksUseCase()).thenReturn(tasksFlow)

        val viewModel = getViewModel()

        assertEquals(
            tasks,
            viewModel.uiState.value.tasks
        )
    }

    @Test
    fun `should generate plan successfully`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            ),
            createTask(
                id = "task-2",
                title = "Buy groceries"
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "Higher priority and should be completed first.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-2",
                reason = "Can be completed after the report.",
                suggestedOrder = 1
            )
        )

        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            null,
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should update loading state while generating plan`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "This task should be completed first.",
                suggestedOrder = 0
            )
        )

        val deferred = CompletableDeferred<Unit>()

        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(tasks)
        )

        doSuspendableAnswer {
            deferred.await()
            suggestions
        }.whenever(
            generateTaskSuggestionsUseCase
        ).invoke(tasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            true,
            viewModel.uiState.value.isLoading
        )

        deferred.complete(Unit)

        advanceUntilIdle()

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should expose error when generating plan fails`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            )
        )

        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenThrow(
            RuntimeException("Failed to generate plan")
        )

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            "Failed to generate plan",
            viewModel.uiState.value.error
        )

        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should not generate plan when tasks are empty`() = runTest {
        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(emptyList())
        )

        val viewModel = getViewModel()

        viewModel.generatePlan()

        verify(
            generateTaskSuggestionsUseCase,
            never()
        ).invoke(any())

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            null,
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should apply suggested task order successfully`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ).copy(position = 0),
            createTask(
                id = "task-2",
                title = "Finish report"
            ).copy(position = 1),
            createTask(
                id = "task-3",
                title = "Completed task"
            ).copy(
                completed = true,
                position = 2
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be done later.",
                suggestedOrder = 1
            )
        )

        whenever(
            observeTasksUseCase()
        ).thenReturn(MutableStateFlow(tasks))

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        viewModel.applyPlan()

        val expectedTasks = listOf(
            tasks[1].copy(position = 0),
            tasks[0].copy(position = 1),
            tasks[2].copy(position = 2)
        )

        verify(reorderTasksUseCase).invoke(expectedTasks)

        assertEquals(
            false,
            viewModel.uiState.value.isApplying
        )

        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            null,
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should update applying state while applying plan`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ).copy(position = 0),
            createTask(
                id = "task-2",
                title = "Finish report"
            ).copy(position = 1)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        val expectedTasks = listOf(
            tasks[1].copy(position = 0),
            tasks[0].copy(position = 1)
        )

        val deferred = CompletableDeferred<Unit>()

        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        doSuspendableAnswer {
            deferred.await()
        }.whenever(
            reorderTasksUseCase
        ).invoke(expectedTasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        viewModel.applyPlan()

        assertEquals(
            true,
            viewModel.uiState.value.isApplying
        )

        deferred.complete(Unit)

        advanceUntilIdle()

        assertEquals(
            false,
            viewModel.uiState.value.isApplying
        )

        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            null,
            viewModel.uiState.value.error
        )

        verify(reorderTasksUseCase).invoke(expectedTasks)
    }

    @Test
    fun `should expose error when applying plan fails`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ).copy(position = 0),
            createTask(
                id = "task-2",
                title = "Finish report"
            ).copy(position = 1)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        val expectedTasks = listOf(
            tasks[1].copy(position = 0),
            tasks[0].copy(position = 1)
        )

        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        doSuspendableAnswer {
            throw RuntimeException("Failed to save task order")
        }.whenever(
            reorderTasksUseCase
        ).invoke(expectedTasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        viewModel.applyPlan()

        advanceUntilIdle()

        assertEquals(
            false,
            viewModel.uiState.value.isApplying
        )

        assertEquals(
            "Failed to save task order",
            viewModel.uiState.value.error
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        verify(reorderTasksUseCase).invoke(expectedTasks)
    }

    @Test
    fun `should not apply incomplete plan`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ).copy(position = 0),
            createTask(
                id = "task-2",
                title = "Finish report"
            ).copy(position = 1),
            createTask(
                id = "task-3",
                title = "Go to the gym"
            ).copy(position = 2)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        whenever(
            observeTasksUseCase()
        ).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        viewModel.applyPlan()

        verify(
            reorderTasksUseCase,
            never()
        ).invoke(any())

        assertEquals(
            "The suggested plan is incomplete. Please generate it again.",
            viewModel.uiState.value.error
        )

        assertEquals(
            false,
            viewModel.uiState.value.isApplying
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should not apply plan with duplicate task ids`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ),
            createTask(
                id = "task-2",
                title = "Finish report"
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "Complete this first.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Complete this later.",
                suggestedOrder = 1
            )
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        viewModel.generatePlan()
        viewModel.applyPlan()

        verify(
            reorderTasksUseCase,
            never()
        ).invoke(any())

        assertEquals(
            "The suggested plan is incomplete. Please generate it again.",
            viewModel.uiState.value.error
        )

        assertEquals(
            false,
            viewModel.uiState.value.isApplying
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should emit PlanApplied event when plan is applied successfully`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ).copy(position = 0),
            createTask(
                id = "task-2",
                title = "Finish report"
            ).copy(position = 1)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        val events = mutableListOf<PlanMyDayUiEvent>()

        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler)
        ) {
            viewModel.uiEvent.collect { event ->
                events.add(event)
            }
        }

        viewModel.generatePlan()
        viewModel.applyPlan()

        advanceUntilIdle()

        assertEquals(
            listOf(PlanMyDayUiEvent.PlanApplied),
            events
        )

        verify(reorderTasksUseCase).invoke(
            listOf(
                tasks[1].copy(position = 0),
                tasks[0].copy(position = 1)
            )
        )
    }

    @Test
    fun `should not apply plan when tasks change after generation`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Buy groceries"
            ).copy(
                priority = Priority.MEDIUM,
                position = 0
            ),
            createTask(
                id = "task-2",
                title = "Finish report"
            ).copy(
                priority = Priority.HIGH,
                position = 1
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        val tasksFlow = MutableStateFlow(tasks)

        whenever(observeTasksUseCase()).thenReturn(tasksFlow)

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        advanceUntilIdle()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        val updatedTasks = tasks.map { task ->
            if (task.id == "task-1") {
                task.copy(priority = Priority.HIGH)
            } else {
                task
            }
        }

        tasksFlow.value = updatedTasks

        advanceUntilIdle()

        assertEquals(
            updatedTasks,
            viewModel.uiState.value.tasks
        )

        viewModel.applyPlan()

        advanceUntilIdle()

        verify(
            reorderTasksUseCase,
            never()
        ).invoke(any())

        assertEquals(
            "Your tasks have changed. Please generate a new plan.",
            viewModel.uiState.value.error
        )

        assertEquals(
            false,
            viewModel.uiState.value.isApplying
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should not generate another plan while already loading`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "Complete this task first.",
                suggestedOrder = 0
            )
        )

        val deferred = CompletableDeferred<List<TaskSuggestion>>()

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        doSuspendableAnswer {
            deferred.await()
        }.whenever(
            generateTaskSuggestionsUseCase
        ).invoke(tasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            true,
            viewModel.uiState.value.isLoading
        )

        viewModel.generatePlan()

        verify(
            generateTaskSuggestionsUseCase,
            times(1)
        ).invoke(tasks)

        deferred.complete(suggestions)

        advanceUntilIdle()

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            null,
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should prevent duplicate requests before coroutine starts`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        Dispatchers.setMain(dispatcher)

        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "Complete this task first.",
                suggestedOrder = 0
            )
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val viewModel = getViewModel()

        runCurrent()

        assertEquals(
            tasks,
            viewModel.uiState.value.tasks
        )

        viewModel.generatePlan()

        assertEquals(
            true,
            viewModel.uiState.value.isLoading
        )

        viewModel.generatePlan()

        advanceUntilIdle()

        verify(
            generateTaskSuggestionsUseCase,
            times(1)
        ).invoke(tasks)

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            null,
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should discard suggestions when tasks change during generation`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            ).copy(
                priority = Priority.HIGH,
                position = 0
            ),
            createTask(
                id = "task-2",
                title = "Buy groceries"
            ).copy(
                priority = Priority.MEDIUM,
                position = 1
            )
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-1",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-2",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        val tasksFlow = MutableStateFlow(tasks)

        val deferred = CompletableDeferred<List<TaskSuggestion>>()

        whenever(observeTasksUseCase()).thenReturn(tasksFlow)

        doSuspendableAnswer {
            deferred.await()
        }.whenever(
            generateTaskSuggestionsUseCase
        ).invoke(tasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            true,
            viewModel.uiState.value.isLoading
        )

        val updatedTasks = tasks.map { task ->
            if (task.id == "task-2") {
                task.copy(priority = Priority.HIGH)
            } else {
                task
            }
        }

        tasksFlow.value = updatedTasks

        advanceUntilIdle()

        assertEquals(
            updatedTasks,
            viewModel.uiState.value.tasks
        )

        deferred.complete(suggestions)

        advanceUntilIdle()

        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            "Your tasks have changed. Please generate a new plan.",
            viewModel.uiState.value.error
        )

        assertEquals(
            updatedTasks,
            viewModel.uiState.value.tasks
        )

        verify(
            generateTaskSuggestionsUseCase,
            times(1)
        ).invoke(tasks)
    }

    @Test
    fun `should propagate cancellation when applying plan`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            ).copy(position = 0),
            createTask(
                id = "task-2",
                title = "Buy groceries"
            ).copy(position = 1)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Complete this first.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Complete this later.",
                suggestedOrder = 1
            )
        )

        val reorderedTasks = listOf(
            tasks[1].copy(position = 0),
            tasks[0].copy(position = 1)
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val cancellation = CancellationException(
            "Reordering was cancelled"
        )

        doSuspendableAnswer {
            throw cancellation
        }.whenever(reorderTasksUseCase).invoke(reorderedTasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        viewModel.applyPlan()

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)

        assertEquals(
            suggestions,
            viewModel.uiState.value.suggestions
        )

        verify(
            reorderTasksUseCase,
            times(1)
        ).invoke(reorderedTasks)
    }

    @Test
    fun `should not expose error when plan generation is cancelled`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            )
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        doSuspendableAnswer {
            throw CancellationException("Generation cancelled")
        }.whenever(
            generateTaskSuggestionsUseCase
        ).invoke(tasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()

        advanceUntilIdle()

        assertEquals(
            false,
            viewModel.uiState.value.isLoading
        )

        assertNull(viewModel.uiState.value.error)

        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should not generate plan when all tasks are completed`() = runTest {
        val tasks = listOf(
            createTask(
                id = "task-1",
                title = "Finish report"
            ).copy(completed = true),
            createTask(
                id = "task-2",
                title = "Buy groceries"
            ).copy(completed = true)
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        val viewModel = getViewModel()

        viewModel.generatePlan()

        verify(
            generateTaskSuggestionsUseCase,
            never()
        ).invoke(any())

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(emptyList<TaskSuggestion>(), viewModel.uiState.value.suggestions)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `should not apply plan twice while already applying`() = runTest {
        val tasks = listOf(
            createTask("task-1", "Buy groceries").copy(position = 0),
            createTask("task-2", "Finish report").copy(position = 1)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        val reorderedTasks = listOf(
            tasks[1].copy(position = 0),
            tasks[0].copy(position = 1)
        )

        val deferred = CompletableDeferred<Unit>()

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        doSuspendableAnswer {
            deferred.await()
        }.whenever(reorderTasksUseCase).invoke(reorderedTasks)

        val viewModel = getViewModel()

        viewModel.generatePlan()
        viewModel.applyPlan()

        assertEquals(true, viewModel.uiState.value.isApplying)

        viewModel.applyPlan()

        verify(
            reorderTasksUseCase,
            times(1)
        ).invoke(reorderedTasks)

        deferred.complete(Unit)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isApplying)
        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )
    }

    @Test
    fun `should prevent duplicate apply requests before coroutine starts`() = runTest {
        val tasks = listOf(
            createTask("task-1", "Buy groceries").copy(position = 0),
            createTask("task-2", "Finish report").copy(position = 1)
        )

        val suggestions = listOf(
            TaskSuggestion(
                taskId = "task-2",
                reason = "Higher priority.",
                suggestedOrder = 0
            ),
            TaskSuggestion(
                taskId = "task-1",
                reason = "Can be completed later.",
                suggestedOrder = 1
            )
        )

        val expectedTasks = listOf(
            tasks[1].copy(position = 0),
            tasks[0].copy(position = 1)
        )

        whenever(observeTasksUseCase()).thenReturn(
            MutableStateFlow(tasks)
        )

        whenever(
            generateTaskSuggestionsUseCase(tasks)
        ).thenReturn(suggestions)

        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)

        val viewModel = getViewModel()

        runCurrent()

        viewModel.generatePlan()
        runCurrent()

        assertEquals(suggestions, viewModel.uiState.value.suggestions)

        viewModel.applyPlan()
        viewModel.applyPlan()

        advanceUntilIdle()

        verify(
            reorderTasksUseCase,
            times(1)
        ).invoke(expectedTasks)

        assertEquals(false, viewModel.uiState.value.isApplying)
        assertEquals(
            emptyList<TaskSuggestion>(),
            viewModel.uiState.value.suggestions
        )
    }

    private fun createTask(
        id: String,
        title: String
    ): Task {
        return Task(
            id = id,
            title = title,
            description = null,
            dueDate = LocalDate.now(),
            priority = Priority.MEDIUM,
            category = TaskCategory.PERSONAL,
            completed = false,
            position = 0
        )
    }

    private fun getViewModel() = PlanMyDayViewModel(
            observeTasksUseCase = observeTasksUseCase,
            generateTaskSuggestionsUseCase = generateTaskSuggestionsUseCase,
            reorderTasksUseCase = reorderTasksUseCase
        )
}