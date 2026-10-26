package com.danielvilha.lifepilot.feature.home.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayEmptyStateWhenThereAreNoTasks() {
        val uiState = HomeUiState(
            tasks = emptyList()
        )

        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = uiState,
                    onCreateTask = {},
                    onEditTask = {},
                    onPlanMyDay = {},
                    onTasksReordered = {},
                    onToggleCompleted = {},
                    onDeleteTask = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }

        composeTestRule
            .onNodeWithText("Today's Tasks")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Daily Progress")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("0%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("0 of 0 tasks completed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Your day starts here!")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Create your first task and start making progress."
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Plan My Day")
            .assertDoesNotExist()
    }

    @Test
    fun shouldDisplayTasksAndPlanMyDayButton() {
        val tasks = listOf(
            Task(
                id = "task-1",
                title = "Review pull requests",
                description = null,
                dueDate = null,
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                position = 0,
                completed = true
            ),
            Task(
                id = "task-2",
                title = "Fix Android app bug",
                description = null,
                dueDate = null,
                priority = Priority.HIGH,
                category = TaskCategory.WORK,
                position = 1,
                completed = false
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = HomeUiState(tasks = tasks),
                    onCreateTask = {},
                    onEditTask = {},
                    onPlanMyDay = {},
                    onTasksReordered = {},
                    onToggleCompleted = {},
                    onDeleteTask = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }

        composeTestRule
            .onNodeWithText("Today's Tasks")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("50%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("1 of 2 tasks completed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Plan My Day")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Review pull requests")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Fix Android app bug")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Your day starts here!")
            .assertDoesNotExist()
    }

    @Test
    fun shouldCallOnToggleCompletedWhenTaskCheckboxIsClicked() {
        val task = Task(
            id = "task-1",
            title = "Review pull requests",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            position = 0,
            completed = false
        )

        var toggledTask: Task? = null

        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = HomeUiState(tasks = listOf(task)),
                    onCreateTask = {},
                    onEditTask = {},
                    onPlanMyDay = {},
                    onTasksReordered = {},
                    onToggleCompleted = { selectedTask ->
                        toggledTask = selectedTask
                    },
                    onDeleteTask = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }

        composeTestRule
            .onNode(
                isToggleable()
            )
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(task.id, toggledTask?.id)
            assertEquals(false, toggledTask?.completed)
        }
    }

    @Test
    fun shouldUpdateDailyProgressWhenTaskCompletionChanges() {
        val task = Task(
            id = "task-1",
            title = "Review pull requests",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            position = 0,
            completed = false
        )

        var uiState by mutableStateOf(
            HomeUiState(tasks = listOf(task))
        )

        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = uiState,
                    onCreateTask = {},
                    onEditTask = {},
                    onPlanMyDay = {},
                    onTasksReordered = {},
                    onToggleCompleted = {},
                    onDeleteTask = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }

        composeTestRule
            .onNodeWithText("0%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("0 of 1 tasks completed")
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            uiState = HomeUiState(
                tasks = listOf(task.copy(completed = true))
            )
        }

        composeTestRule
            .onNodeWithText("100%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("1 of 1 tasks completed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("0%")
            .assertDoesNotExist()
    }

    @Test
    fun shouldCallOnPlanMyDayWhenButtonIsClicked() {
        val task = Task(
            id = "task-1",
            title = "Review pull requests",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            position = 0,
            completed = false
        )

        var planMyDayClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                HomeContent(
                    uiState = HomeUiState(
                        tasks = listOf(task)
                    ),
                    onCreateTask = {},
                    onEditTask = {},
                    onPlanMyDay = {
                        planMyDayClicked = true
                    },
                    onTasksReordered = {},
                    onToggleCompleted = {},
                    onDeleteTask = {},
                    snackbarHostState = SnackbarHostState()
                )
            }
        }

        composeTestRule
            .onNodeWithText("Plan My Day")
            .performClick()

        composeTestRule.runOnIdle {
            assertTrue(planMyDayClicked)
        }
    }
}