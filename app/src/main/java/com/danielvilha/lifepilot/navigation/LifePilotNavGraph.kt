package com.danielvilha.lifepilot.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danielvilha.lifepilot.feature.ai.presentation.AiScreen
import com.danielvilha.lifepilot.feature.edit.presentation.TaskEditRoute
import com.danielvilha.lifepilot.feature.home.presentation.HomeScreen

@Composable
fun LifePilotNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable<Screen.Home> {
            HomeScreen(
                onCreateTask = {
                    navController.navigate(Screen.Ai)
                },
                onEditTask = { taskId ->
                    navController.navigate(
                        Screen.EditTask(taskId)
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