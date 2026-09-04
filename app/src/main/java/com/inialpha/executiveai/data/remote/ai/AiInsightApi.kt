package com.inialpha.executiveai.data.remote.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiInsightApi {
    /**
     * Sends every selected/retrieved email for an account in a single request — no client-side
     * chunking (per REQUIREMENTS.md; the backend fans the batch out to Groq itself).
     */
    @POST("extract-insights-from-emails/")
    suspend fun extractInsights(@Body request: InsightRequestDto): Response<List<InsightResponseDto>>

    companion object {
        const val BASE_URL = "https://emailmanager-hz68.onrender.com/"
    }
}
