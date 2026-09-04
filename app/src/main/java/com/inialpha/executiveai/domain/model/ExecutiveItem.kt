package com.inialpha.executiveai.domain.model

/**
 * The unified "executive action" model. Every event, task, deadline, and reminder the AI
 * extracts from an email becomes a [ExecutiveItem] in the PROPOSED state. Nothing here is a
 * real commitment (calendar event, scheduled alarm) until the user explicitly accepts it.
 *
 * This is the single model that the Upcoming, Tasks, and Reminders screens all read from,
 * filtered by [type] and [state].
 */
data class ExecutiveItem(
    val id: String,
    val sourceEmailId: String?,
    val sourceThreadId: String?,
    val accountId: String,
    val type: ExecutiveItemType,
    val title: String,
    val description: String?,
    val location: String?,
    /** Epoch millis for the item's relevant date/time (event start, deadline, reminder fire time). Nullable for undated tasks. */
    val dueAtMillis: Long?,
    val state: ExecutiveItemState,
    val createdAt: Long,
    val updatedAt: Long,
    /** Set once the user accepts an EVENT item and it is mirrored into Google Calendar / a REMINDER item is scheduled as an Android alarm. */
    val executionRef: String?,
)

enum class ExecutiveItemType {
    EVENT,
    TASK,
    DEADLINE,
    REMINDER,
    VOICE_COMMAND,
}

/**
 * Information → Understanding → Decision → Commitment → Reminder → Execution.
 * PROPOSED/EDITED are both pre-decision states surfaced for user review;
 * ACCEPTED is the "Commitment" step and is what triggers scheduling/calendar creation;
 * COMPLETED is "Execution" finished; REJECTED means discarded.
 */
enum class ExecutiveItemState {
    PROPOSED,
    EDITED,
    ACCEPTED,
    REJECTED,
    COMPLETED,
}
