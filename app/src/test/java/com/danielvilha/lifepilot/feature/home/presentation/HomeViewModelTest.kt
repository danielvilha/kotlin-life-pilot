package com.danielvilha.lifepilot.feature.home.presentation

import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.usecase.DeleteTaskUseCase
import com.danielvilha.lifepilot.domain.usecase.ObserveTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.ReorderTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.UpdateTaskCompletionUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var observeTasksUseCase: ObserveTasksUseCase
    private lateinit var reorderTasksUseCase: ReorderTasksUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var updateTaskCompletionUseCase: UpdateTaskCompletionUseCase

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        observeTasksUseCase = mock()
        reorderTasksUseCase = mock()
        deleteTaskUseCase = mock()
        updateTaskCompletionUseCase = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTasks(
        count: Int,
        completed: Boolean = false
    ): List<Task> {
        return List(count) { index ->
            Task(
                id = "${index + 1}",
                title = "Task ${index + 1}",
                description = "Description ${index + 1}",
                dueDate = LocalDate.of(2026, 9, 26).plusDays(index.toLong()),
                priority = Priority.MEDIUM,
                category = TaskCategory.PERSONAL,
                completed = completed,
                position = index
            )
        }
    }

    private fun createViewModel() = HomeViewModel(
        observeTasksUseCase = observeTasksUseCase,
        reorderTasksUseCase = reorderTasksUseCase,
        deleteTaskUseCase = deleteTaskUseCase,
        updateTaskCompletionUseCase = updateTaskCompletionUseCase,
    )

    @Test
    fun `should expose observed tasks in ui state`() = runTest {
        val tasks = createTasks(2)

        whenever(observeTasksUseCase()).thenReturn(flowOf(tasks))

        viewModel = createViewModel()

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        try {
            assertEquals(tasks, viewModel.uiState.value.tasks)
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun `should delete task and update deleting state`() = runTest {
        val task = createTasks(1).first()

        val deleteStarted = CompletableDeferred<Unit>()
        val allowDeleteToFinish = CompletableDeferred<Unit>()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(task)))
        whenever(deleteTaskUseCase(org.mockito.kotlin.any())).doSuspendableAnswer {
            deleteStarted.complete(Unit)
            allowDeleteToFinish.await()
        }

        viewModel = createViewModel()

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        try {
            viewModel.deleteTask(task)

            deleteStarted.await()

            assertTrue(viewModel.uiState.value.isDeleting)

            allowDeleteToFinish.complete(Unit)

            testScheduler.advanceUntilIdle()

            verify(deleteTaskUseCase).invoke(task)
            assertFalse(viewModel.uiState.value.isDeleting)
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun `should emit success message when task is deleted`() = runTest {
        val task = createTasks(1).first()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(task)))

        viewModel = createViewModel()

        val events = mutableListOf<HomeUiEvent>()

        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                events.add(event)
            }
        }

        try {
            viewModel.deleteTask(task)

            testScheduler.advanceUntilIdle()

            assertEquals(
                HomeUiEvent.ShowMessage("Task deleted"),
                events.first()
            )
        } finally {
            eventsJob.cancel()
        }
    }

    @Test
    fun `should emit error message when task deletion fails`() = runTest {
        val task = createTasks(1).first()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(task)))

        whenever(
            deleteTaskUseCase(org.mockito.kotlin.any())
        ).doSuspendableAnswer {
            throw RuntimeException("Database error")
        }

        viewModel = createViewModel()

        val events = mutableListOf<HomeUiEvent>()

        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                events.add(event)
            }
        }

        try {
            viewModel.deleteTask(task)

            testScheduler.advanceUntilIdle()

            verify(deleteTaskUseCase).invoke(task)

            assertEquals(
                HomeUiEvent.ShowMessage("Database error"),
                events.first()
            )

            assertFalse(viewModel.uiState.value.isDeleting)
        } finally {
            eventsJob.cancel()
        }
    }

    @Test
    fun `should mark incomplete task as completed`() = runTest {
        val task = createTasks(1).first()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(task)))

        viewModel = createViewModel()

        viewModel.toggleTaskCompleted(task)

        testScheduler.advanceUntilIdle()

        verify(updateTaskCompletionUseCase).invoke(
            task = task,
            completed = true
        )
    }

    @Test
    fun `should mark completed task as incomplete`() = runTest {
        val completedTask = createTasks(1, true).first()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(completedTask)))

        viewModel = createViewModel()

        viewModel.toggleTaskCompleted(completedTask)

        testScheduler.advanceUntilIdle()

        verify(updateTaskCompletionUseCase).invoke(
            task = completedTask,
            completed = false
        )
    }

    @Test
    fun `should update completion loading state while task is being updated`() = runTest {
        val task = createTasks(1).first()

        val updateStarted = CompletableDeferred<Unit>()
        val allowUpdateToFinish = CompletableDeferred<Unit>()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(task)))

        whenever(
            updateTaskCompletionUseCase(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()
            )
        ).doSuspendableAnswer {
            updateStarted.complete(Unit)
            allowUpdateToFinish.await()
        }

        viewModel = createViewModel()

        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        try {
            viewModel.toggleTaskCompleted(task)

            updateStarted.await()

            assertTrue(
                viewModel.uiState.value.isUpdatingCompletion
            )

            allowUpdateToFinish.complete(Unit)

            testScheduler.advanceUntilIdle()

            verify(updateTaskCompletionUseCase).invoke(
                task = task,
                completed = true
            )

            assertFalse(
                viewModel.uiState.value.isUpdatingCompletion
            )
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun `should emit error message when task completion update fails`() = runTest {
        val task = createTasks(1).first()

        whenever(observeTasksUseCase()).thenReturn(flowOf(listOf(task)))

        whenever(
            updateTaskCompletionUseCase(
                org.mockito.kotlin.any(),
                org.mockito.kotlin.any()
            )
        ).doSuspendableAnswer {
            throw RuntimeException("Failed to update completion")
        }

        viewModel = createViewModel()

        val events = mutableListOf<HomeUiEvent>()

        val eventsJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { event ->
                events.add(event)
            }
        }

        try {
            viewModel.toggleTaskCompleted(task)

            testScheduler.advanceUntilIdle()

            verify(updateTaskCompletionUseCase).invoke(
                task = task,
                completed = true
            )

            assertEquals(
                HomeUiEvent.ShowMessage("Failed to update completion"),
                events.first()
            )

            assertFalse(
                viewModel.uiState.value.isUpdatingCompletion
            )
        } finally {
            eventsJob.cancel()
        }
    }

    @Test
    fun `should reorder tasks`() = runTest {
        val tasks = createTasks(2)

        whenever(observeTasksUseCase()).thenReturn(flowOf(tasks))

        viewModel = createViewModel()

        viewModel.onTasksReordered(tasks)

        testScheduler.advanceUntilIdle()

        verify(reorderTasksUseCase).invoke(tasks)
    }
}