package com.danielvilha.lifepilot.feature.ai.data

import com.danielvilha.lifepilot.core.network.Content
import com.danielvilha.lifepilot.core.network.GeminiApi
import com.danielvilha.lifepilot.core.network.GeminiRequest
import com.danielvilha.lifepilot.core.network.GeminiResponse
import com.danielvilha.lifepilot.core.network.Candidate
import com.danielvilha.lifepilot.core.network.Part
import com.danielvilha.lifepilot.domain.model.Priority
import com.danielvilha.lifepilot.domain.model.TaskCategory
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GeminiTaskParserTest {

    private lateinit var geminiApi: GeminiApi
    private val gson = Gson()
    private lateinit var parser: GeminiTaskParser

    @Before
    fun setup() {
        geminiApi = mock()
        parser = GeminiTaskParser(geminiApi, gson)
    }

    @Test
    fun `parseTask should send valid GeminiRequest schema with non-list types and nullable fields`() = runTest {
        val jsonOutput = """
            {
              "tasks": [
                {
                  "title": "Buy groceries",
                  "description": "Buy milk and eggs",
                  "dueDate": "2026-03-30",
                  "priority": "HIGH",
                  "category": "SHOPPING"
                }
              ]
            }
        """.trimIndent()

        val mockResponse = GeminiResponse(
            candidates = listOf(
                Candidate(
                    content = Content(
                        parts = listOf(
                            Part(text = jsonOutput)
                        )
                    )
                )
            )
        )

        whenever(geminiApi.createInteraction(eq("gemini-3.8-flash"), any(), any()))
            .thenReturn(mockResponse)

        val result = parser.parseTask("Buy milk and eggs tomorrow")

        val captor = argumentCaptor<GeminiRequest>()
        verify(geminiApi).createInteraction(eq("gemini-3.8-flash"), any(), captor.capture())

        val request = captor.firstValue
        val schema = request.generationConfig?.responseSchema
        assertTrue("schema must not be null", schema != null)

        val properties = (schema!!["properties"] as Map<*, *>)["tasks"] as Map<*, *>
        val items = properties["items"] as Map<*, *>
        val itemProps = items["properties"] as Map<*, *>

        val descriptionProp = itemProps["description"] as Map<*, *>
        assertEquals("STRING", descriptionProp["type"])
        assertEquals(true, descriptionProp["nullable"])

        val dueDateProp = itemProps["dueDate"] as Map<*, *>
        assertEquals("STRING", dueDateProp["type"])
        assertEquals(true, dueDateProp["nullable"])

        assertEquals(1, result.size)
        assertEquals("Buy groceries", result[0].title)
        assertEquals("Buy milk and eggs", result[0].description)
        assertEquals(Priority.HIGH, result[0].priority)
        assertEquals(TaskCategory.SHOPPING, result[0].category)
    }
}
