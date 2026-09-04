package com.inialpha.executiveai.data.remote.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request contract for POST /extract-insights-from-emails/ — matches REQUIREMENTS.md exactly. */
@Serializable
data class InsightRequestDto(
    @SerialName("current_datetime") val currentDatetime: String,
    @SerialName("emails") val emails: List<EmailPayloadDto>,
)

@Serializable
data class EmailPayloadDto(
    @SerialName("id") val id: String,
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender") val sender: String,
    @SerialName("subject") val subject: String,
    @SerialName("content") val content: String,
    @SerialName("snippet") val snippet: String,
)

@Serializable
data class InsightEventDto(
    @SerialName("title") val title: String,
    @SerialName("date") val date: String? = null,
    @SerialName("time") val time: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("description") val description: String? = null,
)

@Serializable
data class InsightActionDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
)

@Serializable
data class InsightDeadlineDto(
    @SerialName("title") val title: String,
    @SerialName("date") val date: String? = null,
    @SerialName("description") val description: String? = null,
)

@Serializable
data class InsightReminderDto(
    @SerialName("title") val title: String,
    @SerialName("datetime") val datetime: String? = null,
    @SerialName("reason") val reason: String? = null,
)

/**
 * Response contract for a single email's structured intelligence, per REQUIREMENTS.md.
 * The endpoint is sent a batch of emails and processes them individually/sequentially, so the
 * top-level HTTP response is modeled as a JSON array of this object — one entry per email that
 * the backend successfully processed. A malformed/failed individual email is simply expected to
 * be absent from the array (handled defensively — see AiInsightRepository).
 */
@Serializable
data class InsightResponseDto(
    @SerialName("id") val id: String,
    @SerialName("thread_id") val threadId: String,
    @SerialName("sender") val sender: String,
    @SerialName("subject") val subject: String,
    @SerialName("is_important") val isImportant: Boolean = false,
    @SerialName("summary") val summary: String = "",
    @SerialName("events") val events: List<InsightEventDto> = emptyList(),
    @SerialName("actions") val actions: List<InsightActionDto> = emptyList(),
    @SerialName("deadlines") val deadlines: List<InsightDeadlineDto> = emptyList(),
    @SerialName("reminders") val reminders: List<InsightReminderDto> = emptyList(),
)
