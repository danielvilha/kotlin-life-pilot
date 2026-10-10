package com.danielvilha.lifepilot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danielvilha.lifepilot.feature.ai.presentation.AiScreen
import com.danielvilha.lifepilot.feature.edit.presentation.TaskEditRoute
import com.danielvilha.lifepilot.feature.home.presentation.HomeScreen
import com.danielvilha.lifepilot.feature.plan.presentation.PlanMyDayScreen

private const val PLAN_APPLIED_KEY = "plan_applied"

@Composable
fun LifePilotNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable<Screen.Home> { backStackEntry ->

            val planApplied by backStackEntry.savedStateHandle
                .getStateFlow(PLAN_APPLIED_KEY, false)
                .collectAsStateWithLifecycle()

            HomeScreen(
                onCreateTask = {
                    navController.navigate(Screen.Ai)
                },
                onEditTask = { taskId ->
                    navController.navigate(Screen.EditTask(taskId))
                },
                onPlanMyDay = {
                    navController.navigate(Screen.PlanMyDay)
                },
                planApplied = planApplied,
                onPlanAppliedConsumed = {
                    backStackEntry.savedStateHandle.remove<Boolean>(
                        PLAN_APPLIED_KEY
                    )
                }
            )
        }

        composable<Screen.Ai> {
            AiScreen(
                onBack = {
                    navController.navigateUp()
                },
                onTaskCreated = {
                    navController.navigateUp()
                }
            )
        }

        composable<Screen.PlanMyDay> {
            PlanMyDayScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onPlanApplied = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(PLAN_APPLIED_KEY, true)

                    navController.popBackStack()
                }
            )
        }

        composable<Screen.EditTask> {
            TaskEditRoute(
                onBack = {
                    navController.navigateUp()
                },
                onTaskUpdated = {
                    navController.navigateUp()
                }
            )
        }
    }
}