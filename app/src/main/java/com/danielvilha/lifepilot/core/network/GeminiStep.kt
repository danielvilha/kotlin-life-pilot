package com.danielvilha.lifepilot.core.network

data class GeminiStep(
    val type: String?,
    val status: String?,
    val content: List<GeminiContent>?
)
