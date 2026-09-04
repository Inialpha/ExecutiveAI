package com.inialpha.executiveai.domain.model

/**
 * A Gmail message synchronized locally. [id] and [threadId] are the authoritative Gmail
 * identifiers and must always be preserved so AI insights and user actions can be traced
 * back to the original Gmail message/thread.
 *
 * [content] holds a bounded, plain-text extraction of the message body (not the full raw
 * MIME payload) to avoid storing unnecessary raw email content on-device.
 */
data class EmailMessage(
    val id: String,
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
    val hasInsight: Boolean,
)
