package com.danielvilha.lifepilot.core.network

data class GeminiResponse(
    val id: String?,
    val status: String?,
    val steps: List<GeminiStep>?
)
