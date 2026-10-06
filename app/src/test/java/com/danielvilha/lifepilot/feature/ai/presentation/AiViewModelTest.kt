package com.danielvilha.lifepilot.feature.ai.presentation

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.usecase.CreateTasksUseCase
import com.danielvilha.lifepilot.domain.usecase.ParseTaskUseCase
import com.danielvilha.lifepilot.feature.edit.presentation.TaskEditForm
import com.danielvilha.lifepilot.feature.edit.presentation.toParsedTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AiViewModelTest {

    private lateinit var parseTaskUseCase: ParseTaskUseCase
    private lateinit var createTasksUseCase: CreateTasksUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: AiViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        parseTaskUseCase = mock()
        createTasksUseCase = mock()

        viewModel = AiViewModel(
            parseTaskUseCase = parseTaskUseCase,
            createTasksUseCase = createTasksUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should ignore invalid editing task index`() {
        viewModel.startEditingTask(10)

        assertNull(
            viewModel.uiState.value.editingTaskIndex
        )
    }

    @Test
    fun `should update input`() {
        viewModel.onInputChanged("Buy groceries tomorrow")

        assertEquals(
            "Buy groceries tomorrow",
            viewModel.uiState.value.input
        )
    }

    @Test
    fun `should parse tasks successfully`() = runTest {
        val input = "Buy groceries tomorrow and call the dentist"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            ),
            ParsedTask(
                title = "Call the dentist",
                description = null,
                dueDate = LocalDate.of(2026, 10, 9),
                priority = Priority.MEDIUM,
                category = TaskCategory.HEALTH
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        viewModel.onInputChanged(input)

        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        assertEquals(
            parsedTasks,
            viewModel.uiState.value.parsedTasks
        )

        assertFalse(
            viewModel.uiState.value.isLoading
        )

        assertNull(
            viewModel.uiState.value.error
        )

        verify(parseTaskUseCase).invoke(input)
    }

    @Test
    fun `should update loading state while parsing tasks`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            )
        )

        val parseStarted = CompletableDeferred<Unit>()
        val allowParseToFinish = CompletableDeferred<Unit>()

        whenever(
            parseTaskUseCase(any())
        ).doSuspendableAnswer {
            parseStarted.complete(Unit)

            allowParseToFinish.await()

            parsedTasks
        }

        viewModel.onInputChanged(input)

        viewModel.parseTask()

        parseStarted.await()

        assertTrue(
            viewModel.uiState.value.isLoading
        )

        allowParseToFinish.complete(Unit)

        testScheduler.advanceUntilIdle()

        assertFalse(
            viewModel.uiState.value.isLoading
        )

        assertEquals(
            parsedTasks,
            viewModel.uiState.value.parsedTasks
        )
    }

    @Test
    fun `should expose error when parsing fails`() = runTest {
        val input = "Buy groceries tomorrow"

        whenever(
            parseTaskUseCase(input)
        ).doSuspendableAnswer {
            throw RuntimeException("Failed to parse tasks")
        }

        viewModel.onInputChanged(input)

        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        assertFalse(
            viewModel.uiState.value.isLoading
        )

        assertTrue(
            viewModel.uiState.value.parsedTasks.isEmpty()
        )

        assertEquals(
            "Failed to parse tasks",
            viewModel.uiState.value.error
        )

        verify(parseTaskUseCase).invoke(input)
    }

    @Test
    fun `should start editing task`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            ),
            ParsedTask(
                title = "Call the dentist",
                description = null,
                dueDate = LocalDate.of(2026, 10, 9),
                priority = Priority.MEDIUM,
                category = TaskCategory.HEALTH
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.startEditingTask(1)

        assertEquals(
            1,
            viewModel.uiState.value.editingTaskIndex
        )

        assertNull(
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should cancel editing task`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.startEditingTask(0)

        assertEquals(
            0,
            viewModel.uiState.value.editingTaskIndex
        )

        viewModel.cancelEditingTask()

        assertNull(
            viewModel.uiState.value.editingTaskIndex
        )

        assertNull(
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should save edited task`() = runTest {
        val input = "Buy groceries tomorrow and call the dentist"

        val firstTask = ParsedTask(
            title = "Buy groceries",
            description = null,
            dueDate = LocalDate.of(2026, 10, 7),
            priority = Priority.MEDIUM,
            category = TaskCategory.SHOPPING
        )

        val secondTask = ParsedTask(
            title = "Call the dentist",
            description = null,
            dueDate = LocalDate.of(2026, 10, 9),
            priority = Priority.MEDIUM,
            category = TaskCategory.HEALTH
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(
            listOf(firstTask, secondTask)
        )

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.startEditingTask(1)

        val editedForm = TaskEditForm(
            title = "Call my dentist",
            description = "Schedule annual check-up",
            dueDate = LocalDate.of(2026, 10, 12),
            priority = Priority.HIGH,
            category = TaskCategory.HEALTH
        )

        viewModel.saveEditedTask(editedForm)

        val state = viewModel.uiState.value

        assertEquals(2, state.parsedTasks.size)

        assertEquals(
            firstTask,
            state.parsedTasks[0]
        )

        assertEquals(
            editedForm.toParsedTask(),
            state.parsedTasks[1]
        )

        assertNull(
            state.editingTaskIndex
        )

        assertNull(
            state.error
        )
    }

    @Test
    fun `should remove task`() = runTest {
        val input = "Buy groceries tomorrow and call the dentist"

        val firstTask = ParsedTask(
            title = "Buy groceries",
            description = null,
            dueDate = LocalDate.of(2026, 10, 7),
            priority = Priority.MEDIUM,
            category = TaskCategory.SHOPPING
        )

        val secondTask = ParsedTask(
            title = "Call the dentist",
            description = null,
            dueDate = LocalDate.of(2026, 10, 9),
            priority = Priority.MEDIUM,
            category = TaskCategory.HEALTH
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(
            listOf(firstTask, secondTask)
        )

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.removeTask(0)

        val state = viewModel.uiState.value

        assertEquals(
            listOf(secondTask),
            state.parsedTasks
        )

        assertEquals(
            1,
            state.parsedTasks.size
        )

        assertNull(
            state.error
        )
    }

    @Test
    fun `should ignore invalid remove task index`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTask = ParsedTask(
            title = "Buy groceries",
            description = null,
            dueDate = LocalDate.of(2026, 10, 7),
            priority = Priority.MEDIUM,
            category = TaskCategory.SHOPPING
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(listOf(parsedTask))

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.removeTask(10)

        assertEquals(
            listOf(parsedTask),
            viewModel.uiState.value.parsedTasks
        )
    }

    @Test
    fun `should create all parsed tasks`() = runTest {
        val input = "Buy groceries tomorrow and call the dentist"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            ),
            ParsedTask(
                title = "Call the dentist",
                description = null,
                dueDate = LocalDate.of(2026, 10, 9),
                priority = Priority.MEDIUM,
                category = TaskCategory.HEALTH
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.createTasks()

        testScheduler.advanceUntilIdle()

        verify(createTasksUseCase).invoke(parsedTasks)

        assertFalse(
            viewModel.uiState.value.isSaving
        )

        assertNull(
            viewModel.uiState.value.error
        )
    }

    @Test
    fun `should not create tasks when parsed tasks are empty`() = runTest {
        viewModel.createTasks()

        testScheduler.advanceUntilIdle()

        verify(
            createTasksUseCase,
            never()
        ).invoke(any())
    }

    @Test
    fun `should update saving state while creating tasks`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        val creationStarted = CompletableDeferred<Unit>()
        val allowCreationToFinish = CompletableDeferred<Unit>()

        whenever(
            createTasksUseCase(any())
        ).doSuspendableAnswer {
            creationStarted.complete(Unit)

            allowCreationToFinish.await()

            emptyList()
        }

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        viewModel.createTasks()

        creationStarted.await()

        assertTrue(
            viewModel.uiState.value.isSaving
        )

        allowCreationToFinish.complete(Unit)

        testScheduler.advanceUntilIdle()

        assertFalse(
            viewModel.uiState.value.isSaving
        )

        verify(createTasksUseCase).invoke(parsedTasks)
    }

    @Test
    fun `should emit TasksCreated when tasks are created`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        val events = mutableListOf<AiUiEvent>()

        val eventsJob = launch(testDispatcher) {
            viewModel.events.collect { event ->
                events.add(event)
            }
        }

        viewModel.createTasks()

        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(AiUiEvent.TasksCreated),
            events
        )

        verify(createTasksUseCase).invoke(parsedTasks)

        eventsJob.cancel()
    }

    @Test
    fun `should expose error when task creation fails`() = runTest {
        val input = "Buy groceries tomorrow"

        val parsedTasks = listOf(
            ParsedTask(
                title = "Buy groceries",
                description = null,
                dueDate = LocalDate.of(2026, 10, 7),
                priority = Priority.MEDIUM,
                category = TaskCategory.SHOPPING
            )
        )

        whenever(
            parseTaskUseCase(input)
        ).thenReturn(parsedTasks)

        whenever(
            createTasksUseCase(parsedTasks)
        ).doSuspendableAnswer {
            throw RuntimeException("Failed to create tasks")
        }

        viewModel.onInputChanged(input)
        viewModel.parseTask()

        testScheduler.advanceUntilIdle()

        val events = mutableListOf<AiUiEvent>()

        val eventsJob = launch(testDispatcher) {
            viewModel.events.collect { event ->
                events.add(event)
            }
        }

        viewModel.createTasks()

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isSaving)

        assertEquals(
            "Failed to create tasks",
            state.error
        )

        assertTrue(events.isEmpty())

        verify(createTasksUseCase).invoke(parsedTasks)

        eventsJob.cancel()
    }
}