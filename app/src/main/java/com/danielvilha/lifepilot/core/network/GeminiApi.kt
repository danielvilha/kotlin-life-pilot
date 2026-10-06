package com.danielvilha.lifepilot.core.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {

    @POST("models/{model}:generateContent")
    suspend fun createInteraction(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
