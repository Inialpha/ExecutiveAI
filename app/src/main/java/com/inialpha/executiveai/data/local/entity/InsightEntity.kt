package com.inialpha.executiveai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores one AI insight per email. The nested events/actions/deadlines/reminders lists are
 * persisted as JSON via [com.inialpha.executiveai.data.local.Converters] rather than as
 * separate tables, since they are always read/written as a unit alongside the parent email
 * and never queried independently — normalized, user-actionable copies of them live in
 * [ExecutiveItemEntity] instead once the user reviews them.
 */
@Entity(tableName = "email_insights")
data class InsightEntity(
    @PrimaryKey val emailId: String,
    val threadId: String,
    val accountId: String,
    val sender: String,
    val subject: String,
    val isImportant: Boolean,
    val summary: String,
    val eventsJson: String,
    val actionsJson: String,
    val deadlinesJson: String,
    val remindersJson: String,
    val fetchedAt: Long,
)
