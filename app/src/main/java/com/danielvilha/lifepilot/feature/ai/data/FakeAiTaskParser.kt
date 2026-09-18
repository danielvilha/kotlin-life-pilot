package com.danielvilha.lifepilot.feature.ai.data

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.repository.AiTaskParser
import java.time.LocalDate
import javax.inject.Inject

class FakeAiTaskParser @Inject constructor() : AiTaskParser {

    override suspend fun parseTask(input: String): List<ParsedTask> {
        return listOf(
            ParsedTask(
                title = input,
                description = null,
                dueDate = LocalDate.now(),
                priority = Priority.MEDIUM,
                category = TaskCategory.PERSONAL
            )
        )
    }
}