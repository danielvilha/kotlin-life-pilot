package com.danielvilha.lifepilot.core.network

data class GeminiResponseFormat(
    val type: String,
    val mime_type: String,
    val schema: Map<String, Any>
)
