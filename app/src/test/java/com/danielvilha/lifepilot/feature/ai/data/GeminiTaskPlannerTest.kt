package com.danielvilha.lifepilot.feature.ai.data

import com.danielvilha.lifepilot.core.network.GeminiApi
import com.danielvilha.lifepilot.core.network.GeminiResponse
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.Task
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiTaskPlannerTest {

    private val geminiApi: GeminiApi = mock()
    private val gson = Gson()

    private val planner = GeminiTaskPlanner(
        geminiApi = geminiApi,
        gson = gson
    )

    @Test
    fun `should stop retrying when coroutine is cancelled`() = runTest {
        val tasks = listOf(createTask())

        val response = Response.error<GeminiResponse>(
            429,
            "Too Many Requests".toResponseBody("application/json".toMediaType())
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            throw HttpException(response)
        }

        val job = launch {
            planner.generateSuggestions(tasks)
        }

        runCurrent()

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )

        job.cancelAndJoin()

        advanceTimeBy(10_000.milliseconds)
        runCurrent()

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )

        assertTrue(job.isCancelled)
    }

    @Test
    fun `should retry when Gemini returns HTTP 429`() = runTest {
        val tasks = listOf(createTask())

        val response = Response.error<GeminiResponse>(
            429,
            "Too Many Requests".toResponseBody(
                "application/json".toMediaType()
            )
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            throw HttpException(response)
        }

        var thrownException: Throwable? = null

        val job = launch {
            try {
                planner.generateSuggestions(tasks)
            } catch (e: HttpException) {
                thrownException = e
            }
        }

        advanceUntilIdle()

        verify(
            geminiApi,
            times(4)
        ).createInteraction(
            any(),
            any(),
            any()
        )

        assertTrue(job.isCompleted)
        assertTrue(thrownException is HttpException)
    }

    @Test
    fun `should return suggestions when retry succeeds`() = runTest {
        val tasks = listOf(createTask())

        val errorResponse = Response.error<GeminiResponse>(
            429,
            "Too Many Requests".toResponseBody(
                "application/json".toMediaType()
            )
        )

        val successResponse = Gson().fromJson(
            """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "{\"suggestions\":[{\"taskId\":\"task-1\",\"reason\":\"High priority task.\",\"suggestedOrder\":0}]}"
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenThrow(
            HttpException(errorResponse)
        ).thenReturn(successResponse)

        val suggestions = planner.generateSuggestions(tasks)

        assertEquals(1, suggestions.size)
        assertEquals("task-1", suggestions.first().taskId)
        assertEquals("High priority task.", suggestions.first().reason)
        assertEquals(0, suggestions.first().suggestedOrder)

        verify(
            geminiApi,
            times(2)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should not retry when Gemini returns HTTP 403`() = runTest {
        val tasks = listOf(createTask())

        val errorResponse = Response.error<GeminiResponse>(
            403,
            "Forbidden".toResponseBody(
                "application/json".toMediaType()
            )
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenThrow(HttpException(errorResponse))

        val exception = assertFailsWith<HttpException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(403, exception.code())

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should retry when Gemini returns HTTP 500`() = runTest {
        val tasks = listOf(createTask())

        val errorResponse = Response.error<GeminiResponse>(
            500,
            "Internal Server Error".toResponseBody(
                "application/json".toMediaType()
            )
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenThrow(
            HttpException(errorResponse)
        )

        var thrownException: HttpException? = null

        val job = launch {
            try {
                planner.generateSuggestions(tasks)
            } catch (e: HttpException) {
                thrownException = e
            }
        }

        advanceUntilIdle()

        verify(
            geminiApi,
            times(4)
        ).createInteraction(
            any(),
            any(),
            any()
        )

        assertTrue(job.isCompleted)
        assertEquals(500, thrownException?.code())
    }

    @Test
    fun `should throw exception when Gemini response has no model output`() = runTest {
        val tasks = listOf(createTask())

        val emptyResponse = gson.fromJson(
            """
        {
            "candidates": []
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(emptyResponse)

        val exception = assertFailsWith<IllegalStateException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(
            "Gemini response did not contain model output",
            exception.message
        )

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should throw exception when Gemini returns malformed JSON`() = runTest {
        val tasks = listOf(createTask())

        val malformedResponse = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{invalid json}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(malformedResponse)

        val exception = assertFailsWith<JsonSyntaxException> {
            planner.generateSuggestions(tasks)
        }

        assertNotNull(exception)

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should throw meaningful exception when suggestions field is missing`() = runTest {
        val tasks = listOf(createTask())

        val response = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"message\":\"Unable to generate plan\"}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(response)

        val exception = assertFailsWith<IllegalStateException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(
            "Gemini response did not contain valid suggestions",
            exception.message
        )

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should return empty list when Gemini returns no suggestions`() = runTest {
        val tasks = listOf(createTask())

        val response = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"suggestions\":[]}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(response)

        val suggestions = planner.generateSuggestions(tasks)

        assertTrue(suggestions.isEmpty())

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should reject suggestion when taskId is missing`() = runTest {
        val tasks = listOf(createTask())

        val response = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"suggestions\":[{\"reason\":\"High priority task\",\"suggestedOrder\":1}]}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(response)

        val exception = assertFailsWith<IllegalStateException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(
            "Gemini response contains a suggestion without taskId",
            exception.message
        )

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should reject suggestion when reason is missing`() = runTest {
        val tasks = listOf(createTask())

        val response = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"suggestions\":[{\"taskId\":\"task-1\",\"suggestedOrder\":1}]}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(response)

        val exception = assertFailsWith<IllegalStateException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(
            "Gemini response contains a suggestion without reason",
            exception.message
        )

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should reject suggestion when suggestedOrder is missing`() = runTest {
        val tasks = listOf(createTask())

        val response = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"suggestions\":[{\"taskId\":\"task-1\",\"reason\":\"High priority task\"}]}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(response)

        val exception = assertFailsWith<IllegalStateException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(
            "Gemini response contains a suggestion without suggestedOrder",
            exception.message
        )

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `should reject suggestion when suggestedOrder is negative`() = runTest {
        val tasks = listOf(createTask())

        val response = gson.fromJson(
            """
        {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {
                                "text": "{\"suggestions\":[{\"taskId\":\"task-1\",\"reason\":\"High priority task\",\"suggestedOrder\":-1}]}"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent(),
            GeminiResponse::class.java
        )

        whenever(
            geminiApi.createInteraction(
                any(),
                any(),
                any()
            )
        ).thenReturn(response)

        val exception = assertFailsWith<IllegalStateException> {
            planner.generateSuggestions(tasks)
        }

        assertEquals(
            "Gemini response contains an invalid suggestedOrder",
            exception.message
        )

        verify(
            geminiApi,
            times(1)
        ).createInteraction(
            any(),
            any(),
            any()
        )
    }

    private fun createTask(
        id: String = "task-1",
        title: String = "Finish report"
    ): Task {
        return Task(
            id = id,
            title = title,
            description = null,
            dueDate = null,
            priority = Priority.HIGH,
            category = TaskCategory.PERSONAL,
            completed = false,
            position = 0
        )
    }
}