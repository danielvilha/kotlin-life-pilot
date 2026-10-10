package com.danielvilha.lifepilot.feature.ai.data

import android.util.Log
import com.danielvilha.lifepilot.BuildConfig
import com.danielvilha.lifepilot.core.network.Content
import com.danielvilha.lifepilot.core.network.GeminiApi
import com.danielvilha.lifepilot.core.network.GeminiRequest
import com.danielvilha.lifepilot.core.network.GeminiResponse
import com.danielvilha.lifepilot.core.network.GenerationConfig
import com.danielvilha.lifepilot.core.network.Part
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskSuggestion
import com.google.gson.Gson
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class GeminiTaskPlanner @Inject constructor(
    private val geminiApi: GeminiApi,
    private val gson: Gson
) : AiTaskPlanner {
    override suspend fun generateSuggestions(
        tasks: List<Task>
    ): List<TaskSuggestion> {
        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(
                            text = buildPrompt(tasks)
                        )
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = taskSuggestionsSchema()
            )
        )

        val response = createInteractionWithRetry(
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )

        val outputText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: throw IllegalStateException(
                "Gemini response did not contain model output"
            )

        val suggestionsResponse = gson.fromJson(
            outputText,
            TaskSuggestionsResponse::class.java
        )

        val suggestions = suggestionsResponse?.suggestions
            ?: throw IllegalStateException(
                "Gemini response did not contain valid suggestions"
            )

        return suggestions.map { suggestion ->
            suggestion.toDomain()
        }
    }

    private fun buildPrompt(
        tasks: List<Task>
    ): String {
        val tasksText = tasks.joinToString(
            separator = "\n"
        ) { task ->
            """
            ID: ${task.id}
            Title: ${task.title}
            Description: ${task.description ?: "None"}
            Due date: ${task.dueDate ?: "None"}
            Priority: ${task.priority.name}
            Category: ${task.category.name}
            """.trimIndent()
        }

        return """
            You are LifePilot's task planning assistant.

            Your job is to analyze the user's pending tasks and recommend
            the best order in which they should be completed.

            PLANNING RULES:
            - Consider due date, priority, category, and description.
            - Tasks that are overdue or due sooner should generally receive more urgency.
            - HIGH priority tasks should generally be preferred over MEDIUM and LOW priority tasks.
            - Balance urgency and priority when deciding the order.
            - Use only the task IDs provided below.
            - Do not create new tasks.
            - Include every provided task exactly once.
            - Each taskId must appear exactly once.
            - suggestedOrder must start at 0.
            - suggestedOrder must be unique for every suggestion.
            - Give a short and useful reason for each recommendation.
            - Do not modify the tasks.

            PENDING TASKS:
            $tasksText
        """.trimIndent()
    }

    private fun taskSuggestionsSchema(): Map<String, Any> {
        return mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "suggestions" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "taskId" to mapOf(
                                "type" to "STRING"
                            ),
                            "reason" to mapOf(
                                "type" to "STRING"
                            ),
                            "suggestedOrder" to mapOf(
                                "type" to "INTEGER"
                            )
                        ),
                        "required" to listOf(
                            "taskId",
                            "reason",
                            "suggestedOrder"
                        )
                    )
                )
            ),
            "required" to listOf("suggestions")
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
                    model = "gemini-3.5-flash-lite",
                    apiKey = apiKey,
                    request = request
                )
            } catch (e: HttpException) {
                val errorBody = e.response()
                    ?.errorBody()
                    ?.string()

                try {
                    Log.e(
                        "GeminiTaskPlanner",
                        "HTTP ${e.code()}: $errorBody",
                        e
                    )
                } catch (_: Throwable) {
                    // Ignored in unit tests where Android Log is not mocked
                }

                val retryable =
                    e.code() == 408 ||
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

private data class TaskSuggestionsResponse(
    val suggestions: List<TaskSuggestionDto>?
)

private data class TaskSuggestionDto(
    val taskId: String?,
    val reason: String?,
    val suggestedOrder: Int?
)

private fun TaskSuggestionDto.toDomain(): TaskSuggestion {
    val validTaskId = taskId
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException(
            "Gemini response contains a suggestion without taskId"
        )

    val validReason = reason
        ?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException(
            "Gemini response contains a suggestion without reason"
        )

    val validOrder = suggestedOrder
        ?: throw IllegalStateException(
            "Gemini response contains a suggestion without suggestedOrder"
        )

    if (validOrder < 0) {
        throw IllegalStateException(
            "Gemini response contains an invalid suggestedOrder"
        )
    }

    return TaskSuggestion(
        taskId = validTaskId,
        reason = validReason,
        suggestedOrder = validOrder
    )
}