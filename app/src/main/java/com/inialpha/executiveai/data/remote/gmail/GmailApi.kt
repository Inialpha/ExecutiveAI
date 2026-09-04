package com.inialpha.executiveai.data.remote.gmail

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Direct Gmail REST client used with a per-request OAuth Bearer access token obtained from
 * [com.inialpha.executiveai.data.auth.GoogleAuthManager]. Android owns Gmail retrieval end to
 * end — this call never goes through the EmailManager backend.
 *
 * Scope required: https://www.googleapis.com/auth/gmail.readonly
 */
interface GmailApi {
    @GET("gmail/v1/users/me/messages")
    suspend fun listMessages(
        @Header("Authorization") bearerToken: String,
        @Query("maxResults") maxResults: Int = 25,
        @Query("q") query: String = "in:inbox",
        @Query("pageToken") pageToken: String? = null,
    ): Response<GmailMessageListResponseDto>

    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") bearerToken: String,
        @Path("id") id: String,
        @Query("format") format: String = "full",
    ): Response<GmailMessageDto>

    companion object {
        const val BASE_URL = "https://gmail.googleapis.com/"
        const val SCOPE_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    }
}
