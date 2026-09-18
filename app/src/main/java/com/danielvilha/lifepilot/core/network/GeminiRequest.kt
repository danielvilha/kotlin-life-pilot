package com.danielvilha.lifepilot.core.network

data class GeminiRequest(
    val model: String,
    val input: String,
    val response_format: GeminiResponseFormat
)