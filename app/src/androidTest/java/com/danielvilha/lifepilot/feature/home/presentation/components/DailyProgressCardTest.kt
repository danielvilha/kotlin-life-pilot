package com.danielvilha.lifepilot.feature.home.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danielvilha.lifepilot.domain.model.DailyProgress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyProgressCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldDisplayDailyProgressCorrectly() {
        val progress = DailyProgress(
            totalTasks = 5,
            completedTasks = 3
        )

        composeTestRule.setContent {
            MaterialTheme {
                DailyProgressCard(progress = progress)
            }
        }

        composeTestRule
            .onNodeWithText("Daily Progress")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("60%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("3 of 5 tasks completed")
            .assertIsDisplayed()
    }

    @Test
    fun shouldUpdateProgressWhenCompletedTasksChange() {
        val progress = mutableStateOf(
            DailyProgress(
                totalTasks = 5,
                completedTasks = 2
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                DailyProgressCard(
                    progress = progress.value
                )
            }
        }

        composeTestRule
            .onNodeWithText("40%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("2 of 5 tasks completed")
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            progress.value = DailyProgress(
                totalTasks = 5,
                completedTasks = 3
            )
        }

        composeTestRule
            .onNodeWithText("60%")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("3 of 5 tasks completed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("40%")
            .assertDoesNotExist()
    }
}