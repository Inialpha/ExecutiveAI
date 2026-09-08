package com.inialpha.executiveai.data.remote.ai

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiInsightApi {
    /**
     * Sends exactly one email per request (see InsightRepository). Returns the raw
     * [ResponseBody] rather than an auto-converted DTO — deliberately, for now: this lets
     * InsightRepository capture the exact bytes the backend sent *before* attempting to parse
     * them, so a schema mismatch shows the real raw JSON instead of just an opaque exception.
     * See InsightRepository's EmailProcessingDebugInfo for where that raw text is surfaced.
     */
    @POST("extract-insights-from-emails/")
    suspend fun extractInsightsRaw(@Body request: InsightRequestDto): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://emailmanager-hz68.onrender.com/"
    }
}
