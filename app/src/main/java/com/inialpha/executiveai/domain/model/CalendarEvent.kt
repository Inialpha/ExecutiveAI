package com.inialpha.executiveai.domain.model

/**
 * A Calendar event synchronized read-only from a calendar provider, plus the metadata needed for
 * conflict detection against proposed [ExecutiveItem]s of type EVENT.
 *
 * [provider] identifies which calendar backend the event came from. Only "google" exists today
 * (via [com.inialpha.executiveai.data.repository.CalendarRepository]); the field exists so a
 * future provider (e.g. Outlook) can be added without changing this model or the Calendar UI —
 * see that repository's doc comment for the intended extension point.
 */
data class CalendarEvent(
    val id: String,
    val calendarId: String,
    val accountId: String,
    val provider: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val isAllDay: Boolean,
    val htmlLink: String?,
)
