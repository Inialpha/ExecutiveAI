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

// --- Response DTOs, field-for-field matched against the backend's Pydantic EmailInsight schema
// (2026-09 revision the person supplied). Nullability below mirrors Optional[...] = None exactly;
// fields with no Pydantic default (title, subject, is_important, summary, and each sub-model's
// required description/reason) are non-nullable here on purpose, since Pydantic would reject a
// response missing them — the backend can never legitimately omit them.

@Serializable
data class InsightEventDto(
    @SerialName("title") val title: String,
    @SerialName("date") val date: String? = null,
    @SerialName("time") val time: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("description") val description: String,
)

@Serializable
data class InsightActionDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("due_date") val dueDate: String? = null,
)

@Serializable
data class InsightDeadlineDto(
    @SerialName("title") val title: String,
    @SerialName("date") val date: String? = null,
    @SerialName("description") val description: String,
)

@Serializable
data class InsightReminderDto(
    @SerialName("title") val title: String,
    @SerialName("datetime") val datetime: String? = null,
    @SerialName("reason") val reason: String,
)

/**
 * Response contract for a single email's structured intelligence — matches the backend's Pydantic
 * `EmailInsight` model exactly. NOTE the two fields that were previously (incorrectly) declared
 * non-nullable here: `thread_id` and `sender` are both `Optional[str] = None` on the backend, so a
 * legitimate successful response can send `null` for either. Before this fix, a non-nullable Kotlin
 * field receiving JSON `null` threw a hard SerializationException — caught by the generic
 * exception handler and recorded as an opaque FAILED, even though the backend had genuinely
 * succeeded. This was a real, confirmed schema mismatch, not a guess.
 */
@Serializable
data class InsightResponseDto(
    @SerialName("id") val id: String,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("sender") val sender: String? = null,
    @SerialName("subject") val subject: String,
    @SerialName("is_important") val isImportant: Boolean,
    @SerialName("summary") val summary: String,
    @SerialName("events") val events: List<InsightEventDto> = emptyList(),
    @SerialName("actions") val actions: List<InsightActionDto> = emptyList(),
    @SerialName("deadlines") val deadlines: List<InsightDeadlineDto> = emptyList(),
    @SerialName("reminders") val reminders: List<InsightReminderDto> = emptyList(),
)

/**
 * The actual confirmed wire shape (from a real captured backend response, 2026-09): the backend
 * wraps its result(s) in a top-level object with an "emails" array — not a bare JSON array and
 * not a bare single object, both of which were previously-tried fallback guesses. See
 * InsightRepository.parseResponseBody, which now tries this shape first.
 */
@Serializable
data class InsightBatchResponseDto(
    @SerialName("emails") val emails: List<InsightResponseDto> = emptyList(),
)
