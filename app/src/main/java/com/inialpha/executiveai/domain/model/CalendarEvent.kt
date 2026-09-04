package com.inialpha.executiveai.domain.model

/**
 * A Google Calendar event synchronized read-only from the Calendar API, plus the metadata
 * needed later for conflict detection against proposed [ExecutiveItem]s of type EVENT.
 */
data class CalendarEvent(
    val id: String,
    val calendarId: String,
    val accountId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val isAllDay: Boolean,
    val htmlLink: String?,
)
