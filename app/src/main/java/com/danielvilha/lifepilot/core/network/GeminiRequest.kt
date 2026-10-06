package com.danielvilha.lifepilot.core.network

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: Map<String, Any>? = null
)
