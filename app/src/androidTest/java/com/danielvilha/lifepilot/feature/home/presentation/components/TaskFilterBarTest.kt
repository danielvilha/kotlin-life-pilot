package com.danielvilha.lifepilot.feature.home.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danielvilha.lifepilot.domain.model.TaskFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskFilterBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shouldCallOnFilterSelectedWhenPendingIsClicked() {
        var selectedFilter: TaskFilter? = null

        composeTestRule.setContent {
            MaterialTheme {
                TaskFilterBar(
                    selectedFilter = TaskFilter.ALL,
                    onFilterSelected = { filter ->
                        selectedFilter = filter
                    }
                )
            }
        }

        composeTestRule
            .onNodeWithText("All")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Pending")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Completed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Pending")
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                TaskFilter.PENDING,
                selectedFilter
            )
        }
    }
}