package com.inialpha.executiveai.domain.model

import kotlinx.serialization.Serializable

/**
 * Structured intelligence extracted from a single email by the EmailManager AI gateway.
 * Mirrors the contract documented in REQUIREMENTS.md exactly.
 */
data class EmailInsight(
    val emailId: String,
    val threadId: String,
    val accountId: String,
    val sender: String,
    val subject: String,
    val isImportant: Boolean,
    val summary: String,
    val events: List<InsightEvent>,
    val actions: List<InsightAction>,
    val deadlines: List<InsightDeadline>,
    val reminders: List<InsightReminder>,
    val fetchedAt: Long,
)

@Serializable
data class InsightEvent(
    val title: String,
    val date: String?,
    val time: String?,
    val location: String?,
    val description: String?,
)

@Serializable
data class InsightAction(
    val title: String,
    val description: String?,
    val dueDate: String?,
)

@Serializable
data class InsightDeadline(
    val title: String,
    val date: String?,
    val description: String?,
)

@Serializable
data class InsightReminder(
    val title: String,
    val datetime: String?,
    val reason: String?,
)
