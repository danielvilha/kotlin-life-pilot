package com.danielvilha.lifepilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.danielvilha.lifepilot.navigation.LifePilotNavGraph
import com.danielvilha.lifepilot.ui.theme.LifePilotTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LifePilotTheme {
                LifePilotNavGraph()
            }
        }
    }
}