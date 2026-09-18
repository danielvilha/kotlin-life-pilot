package com.danielvilha.lifepilot.domain.usecase

import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.repository.AiTaskParser
import javax.inject.Inject

class ParseTaskUseCase @Inject constructor(
    private val aiTaskParser: AiTaskParser
) {

    suspend operator fun invoke(input: String): List<ParsedTask> {
        return aiTaskParser.parseTask(input)
    }
}