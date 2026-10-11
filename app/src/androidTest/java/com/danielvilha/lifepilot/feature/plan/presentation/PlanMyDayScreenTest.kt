package com.danielvilha.lifepilot.feature.plan.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.model.TaskSuggestion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanMyDayScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayGeneratePlanButton() {
        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(),
                onGeneratePlanClick = {},
                onApplyPlanClick = {},
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("Generate Plan")
            .assertIsDisplayed()
    }

    @Test
    fun shouldDisableApplyButtonWhenPlanIsOutdated() {
        val originalTask = Task(
            id = "task-1",
            title = "Complete project documentation",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )

        val updatedTask = originalTask.copy(
            title = "Complete updated documentation"
        )

        val suggestion = TaskSuggestion(
            taskId = "task-1",
            reason = "This task should be completed first.",
            suggestedOrder = 0
        )

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(updatedTask),
                    suggestions = listOf(suggestion),
                    planningSnapshot = listOf(originalTask)
                ),
                onGeneratePlanClick = {},
                onApplyPlanClick = {},
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("Your plan needs updating")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Apply This Plan")
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText("Regenerate Plan")
            .assertIsEnabled()
    }

    @Test
    fun shouldEnableApplyButtonWhenPlanIsUpToDate() {
        val task = Task(
            id = "task-1",
            title = "Complete project documentation",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )

        val suggestion = TaskSuggestion(
            taskId = "task-1",
            reason = "This task should be completed first.",
            suggestedOrder = 0
        )

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(task),
                    suggestions = listOf(suggestion),
                    planningSnapshot = listOf(task)
                ),
                onGeneratePlanClick = {},
                onApplyPlanClick = {},
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("Apply This Plan")
            .assertIsEnabled()

        composeTestRule
            .onNodeWithText("Your plan needs updating")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Regenerate Plan")
            .assertIsEnabled()
    }

    @Test
    fun shouldCallApplyPlanCallbackWhenButtonIsClicked() {
        val task = Task(
            id = "task-1",
            title = "Complete project documentation",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )

        val suggestion = TaskSuggestion(
            taskId = "task-1",
            reason = "This task should be completed first.",
            suggestedOrder = 0
        )

        var applyClickCount = 0

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(task),
                    suggestions = listOf(suggestion),
                    planningSnapshot = listOf(task)
                ),
                onGeneratePlanClick = {},
                onApplyPlanClick = {
                    applyClickCount++
                },
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("Apply This Plan")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, applyClickCount)
        }
    }

    @Test
    fun shouldDisableApplyButtonWhileApplyingPlan() {
        val task = Task(
            id = "task-1",
            title = "Complete project documentation",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )

        val suggestion = TaskSuggestion(
            taskId = "task-1",
            reason = "This task should be completed first.",
            suggestedOrder = 0
        )

        var applyClickCount = 0

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(task),
                    suggestions = listOf(suggestion),
                    planningSnapshot = listOf(task),
                    isApplying = true
                ),
                onGeneratePlanClick = {},
                onApplyPlanClick = {
                    applyClickCount++
                },
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("Applying Plan...")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText("Regenerate Plan")
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText("Applying Plan...")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, applyClickCount)
        }
    }

    @Test
    fun shouldShowLoadingAndDisableGenerateButton() {
        val task = Task(
            id = "task-1",
            title = "Complete project documentation",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )

        var generateClickCount = 0

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(task),
                    isLoading = true
                ),
                onGeneratePlanClick = {
                    generateClickCount++
                },
                onApplyPlanClick = {},
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("AI is planning your day...")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Generate Plan")
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText("Generate Plan")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(0, generateClickCount)
        }
    }

    @Test
    fun shouldShowEmptyStateWhenAllTasksAreCompleted() {
        val completedTask = Task(
            id = "task-1",
            title = "Complete project documentation",
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = true,
            position = 0
        )

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(completedTask)
                ),
                onGeneratePlanClick = {},
                onApplyPlanClick = {},
                onBackClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("You don't have any pending tasks to plan.")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Generate Plan")
            .assertIsNotEnabled()

        composeTestRule
            .onNodeWithText("Your AI Plan")
            .assertDoesNotExist()
    }
}