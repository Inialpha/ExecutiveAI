package com.inialpha.executiveai.data.remote.calendar

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Direct Google Calendar REST client, authenticated the same way as [com.inialpha.executiveai.data.remote.gmail.GmailApi]
 * with a per-request Bearer token.
 *
 * Read scope required: https://www.googleapis.com/auth/calendar.readonly
 * Write scope (event creation, requested only once the user accepts a proposed event —
 * see REQUIREMENTS.md section 9): https://www.googleapis.com/auth/calendar.events
 */
interface CalendarApi {
    @GET("calendar/v3/calendars/{calendarId}/events")
    suspend fun listEvents(
        @Header("Authorization") bearerToken: String,
        @Path("calendarId") calendarId: String = "primary",
        @Query("timeMin") timeMin: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime",
    ): Response<CalendarEventListResponseDto>

    @POST("calendar/v3/calendars/{calendarId}/events")
    suspend fun createEvent(
        @Header("Authorization") bearerToken: String,
        @Path("calendarId") calendarId: String = "primary",
        @Body event: CalendarEventCreateDto,
    ): Response<CalendarEventDto>

    companion object {
        const val BASE_URL = "https://www.googleapis.com/"
        const val SCOPE_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
        const val SCOPE_EVENTS = "https://www.googleapis.com/auth/calendar.events"
    }
}
