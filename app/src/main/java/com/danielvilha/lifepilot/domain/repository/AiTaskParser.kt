package com.danielvilha.lifepilot.domain.repository

import com.danielvilha.lifepilot.domain.model.ParsedTask

interface AiTaskParser {

    suspend fun parseTask(input: String): List<ParsedTask>
}