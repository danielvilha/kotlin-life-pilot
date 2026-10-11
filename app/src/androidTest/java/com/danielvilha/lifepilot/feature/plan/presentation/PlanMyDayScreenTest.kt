package com.danielvilha.lifepilot.feature.plan.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
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
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
            )
        }

        composeTestRule
            .onNodeWithText("Generate Plan")
            .assertIsDisplayed()
    }

    @Test
    fun shouldDisableApplyButtonWhenPlanIsOutdated() {
        val originalTask = createTask(
            id = "task-1",
            title = "Complete project documentation"
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
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
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
        val task = createTask(
            id = "task-1",
            title = "Complete project documentation"
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
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
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
        val task = createTask(
            id = "task-1",
            title = "Complete project documentation"
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
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
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
        val task = createTask(
            id = "task-1",
            title = "Complete project documentation"
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
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
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
        val task = createTask(
            id = "task-1",
            title = "Complete project documentation"
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
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
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
        val completedTask = createTask(
            id = "task-1",
            title = "Complete project documentation"
        ).copy(completed = true)

        composeTestRule.setContent {
            PlanMyDayContent(
                uiState = PlanMyDayUiState(
                    tasks = listOf(completedTask)
                ),
                onGeneratePlanClick = {},
                onApplyPlanClick = {},
                onBackClick = {},
                onMoveSuggestion = {_, _ ->}
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

    @Test
    fun shouldDisplayUpdatedOrderAfterReordering() {
        val task = createTask(
            id = "task-1",
            title = "Finish report"
        )

        val suggestion = TaskSuggestion(
            taskId = "task-1",
            reason = "High priority task.",
            suggestedOrder = 0
        )

        composeTestRule.setContent {
            MaterialTheme {
                PlanSuggestionCard(
                    task = task,
                    suggestion = suggestion,
                    displayOrder = 2
                )
            }
        }

        composeTestRule
            .onNodeWithText("3.")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Finish report")
            .assertIsDisplayed()
    }

    @Test
    fun shouldCallMoveSuggestionWhenTaskIsDragged() {
        val tasks = listOf(
            createTask("task-1", "Finish report"),
            createTask("task-2", "Buy groceries"),
            createTask("task-3", "Go to the gym")
        )

        val suggestions = tasks.mapIndexed { index, task ->
            TaskSuggestion(
                taskId = task.id,
                reason = "AI suggestion",
                suggestedOrder = index
            )
        }

        var moveResult: Pair<Int, Int>? = null

        composeTestRule.setContent {
            MaterialTheme {
                PlanMyDayContent(
                    uiState = PlanMyDayUiState(
                        tasks = tasks,
                        suggestions = suggestions,
                        planningSnapshot = tasks
                    ),
                    onGeneratePlanClick = {},
                    onApplyPlanClick = {},
                    onBackClick = {},
                    onMoveSuggestion = { from, to ->
                        moveResult = from to to
                    }
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Reorder suggestion")
            .onFirst()
            .performTouchInput {
                down(center)
                advanceEventTime(
                    viewConfiguration.longPressTimeoutMillis + 100L
                )
                repeat(25) {
                    moveBy(Offset(0f, 40f))
                    advanceEventTime(32L)
                }
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0 to 2, moveResult)
        }
    }

    private fun createTask(id: String, title: String): Task {
        return Task(
            id = id,
            title = title,
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.WORK,
            completed = false,
            position = 0
        )
    }
}