package com.inialpha.executiveai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emails",
    indices = [Index("accountId"), Index("threadId"), Index("receivedAt"), Index("processingStatus")],
)
data class EmailEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val accountId: String,
    val sender: String,
    val senderName: String?,
    val subject: String,
    val snippet: String,
    val content: String,
    val receivedAt: Long,
    val isRead: Boolean,
    val isImportant: Boolean,
    /** Stores [com.inialpha.executiveai.domain.model.EmailProcessingStatus] by name. */
    val processingStatus: String,
)
