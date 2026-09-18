package com.danielvilha.lifepilot.feature.ai.data

import com.danielvilha.lifepilot.core.network.GeminiApi
import com.danielvilha.lifepilot.core.network.GeminiRequest
import com.danielvilha.lifepilot.core.network.GeminiResponseFormat
import com.danielvilha.lifepilot.BuildConfig
import com.danielvilha.lifepilot.core.network.GeminiResponse
import com.danielvilha.lifepilot.core.network.GeminiTask
import com.danielvilha.lifepilot.core.network.GeminiTaskResponse
import com.danielvilha.lifepilot.domain.model.ParsedTask
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.danielvilha.lifepilot.domain.repository.AiTaskParser
import com.google.gson.Gson
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class GeminiTaskParser @Inject constructor(
    private val geminiApi: GeminiApi,
    private val gson: Gson
) : AiTaskParser {

    override suspend fun parseTask(input: String): List<ParsedTask> {
        val request = GeminiRequest(
            model = "gemini-3.8-flash",
            input = buildPrompt(input),
            response_format = GeminiResponseFormat(
                type = "text",
                mime_type = "application/json",
                schema = taskResponseSchema()
            )
        )

        val response = createInteractionWithRetry(
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )

        val outputText = response.steps
            ?.lastOrNull { it.type == "model_output" }
            ?.content
            ?.firstOrNull { it.type == "text" }
            ?.text
            ?: throw IllegalStateException(
                "Gemini response did not contain model output"
            )

        val geminiResponse = gson.fromJson(
            outputText,
            GeminiTaskResponse::class.java
        )

        return geminiResponse.tasks.map { task ->
            task.toParsedTask()
        }
    }

    private fun buildPrompt(input: String): String {
        val today = LocalDate.now()

        return """
            You are LifePilot's task parsing engine.
    
            CURRENT DATE:
            $today
    
            Your job is to convert the user's text into one or more tasks.
    
            DATE RULES:
            - Resolve all relative date expressions using CURRENT DATE.
            - "tomorrow" means CURRENT DATE + 1 day.
            - "today" means CURRENT DATE.
            - "next Monday", "next Tuesday", etc. must be converted to the next occurrence
              of that weekday after CURRENT DATE.
            - "this Friday" means the Friday in the current week when applicable.
            - When a due date is present, dueDate MUST contain the resolved date in YYYY-MM-DD format.
            - Do NOT return null for dueDate when the user's input explicitly contains a date
              or relative date expression.
            - Return null only when no due date can be determined.
    
            TASK RULES:
            - Create one task for each distinct actionable item.
            - Keep titles concise and actionable.
            - Do not invent missing information.
    
            PRIORITY:
            - LOW
            - MEDIUM
            - HIGH
    
            CATEGORY:
            - PERSONAL
            - WORK
            - SHOPPING
            - HEALTH
            - FINANCE
            - OTHER
    
            USER INPUT:
            $input
        """.trimIndent()
    }

    private fun taskResponseSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "tasks" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "title" to mapOf(
                                "type" to "string"
                            ),
                            "description" to mapOf(
                                "type" to listOf("string", "null")
                            ),
                            "dueDate" to mapOf(
                                "type" to listOf("string", "null")
                            ),
                            "priority" to mapOf(
                                "type" to "string",
                                "enum" to listOf(
                                    "LOW",
                                    "MEDIUM",
                                    "HIGH"
                                )
                            ),
                            "category" to mapOf(
                                "type" to "string",
                                "enum" to listOf(
                                    "PERSONAL",
                                    "WORK",
                                    "SHOPPING",
                                    "HEALTH",
                                    "FINANCE",
                                    "OTHER"
                                )
                            )
                        ),
                        "required" to listOf(
                            "title",
                            "description",
                            "dueDate",
                            "priority",
                            "category"
                        )
                    )
                )
            ),
            "required" to listOf("tasks")
        )
    }

    private fun GeminiTask.toParsedTask(): ParsedTask {
        return ParsedTask(
            title = title,
            description = description,
            dueDate = dueDate?.let(LocalDate::parse),
            priority = Priority.valueOf(priority),
            category = TaskCategory.valueOf(category)
        )
    }

    private suspend fun createInteractionWithRetry(
        apiKey: String,
        request: GeminiRequest
    ): GeminiResponse {

        val maxAttempts = 4

        repeat(maxAttempts) { attempt ->
            try {
                return geminiApi.createInteraction(
                    apiKey = apiKey,
                    request = request
                )
            } catch (e: HttpException) {

                val retryable = e.code() == 408 ||
                        e.code() == 429 ||
                        e.code() in 500..599

                if (!retryable || attempt == maxAttempts - 1) {
                    throw e
                }

                val delayMillis =
                    (1000L * (1L shl attempt)) +
                            Random.nextLong(0, 500)

                delay(delayMillis.milliseconds)
            }
        }

        error("Gemini request failed")
    }
}