package com.inialpha.executiveai.domain.model

/**
 * A Gmail message synchronized locally. [id] and [threadId] are the authoritative Gmail
 * identifiers and must always be preserved so AI insights and user actions can be traced
 * back to the original Gmail message/thread.
 *
 * [content] holds a bounded, plain-text extraction of the message body (not the full raw
 * MIME payload) to avoid storing unnecessary raw email content on-device.
 *
 * [processingStatus] is the durable per-email AI-processing state — see [EmailProcessingStatus].
 * A re-synchronization never resets an existing email's status back to PENDING (see
 * [com.inialpha.executiveai.data.repository.EmailRepository.syncAccount]), so COMPLETED emails
 * are never reprocessed and FAILED ones remain retryable.
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
    val processingStatus: EmailProcessingStatus,
)
