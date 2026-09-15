package com.inialpha.executiveai.data.remote.ai

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiInsightApi {
    /**
     * Sends exactly one email per request (see InsightRepository). Returns the raw
     * [ResponseBody] rather than an auto-converted DTO — this lets InsightRepository try the
     * response against several known/possible schemas (a confirmed real quirk of this backend:
     * results arrive wrapped as `{"emails": [...]}`) rather than assuming one fixed shape and
     * failing outright if it doesn't match. See InsightRepository.parseResponseBody.
     */
    @POST("extract-insights-from-emails/")
    suspend fun extractInsightsRaw(@Body request: InsightRequestDto): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://emailmanager-hz68.onrender.com/"
    }
}
