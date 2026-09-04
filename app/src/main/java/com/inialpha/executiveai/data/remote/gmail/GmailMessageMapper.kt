package com.inialpha.executiveai.data.remote.gmail

import android.util.Base64
import com.inialpha.executiveai.domain.model.EmailMessage

/**
 * Turns a raw Gmail API message resource into our domain [EmailMessage].
 * Handles simple and multipart (text/plain preferred, text/html fallback) payloads and never
 * stores anything beyond a reasonably bounded plain-text extraction of the body.
 */
object GmailMessageMapper {

    private const val MAX_CONTENT_CHARS = 20_000

    fun toDomain(accountId: String, dto: GmailMessageDto): EmailMessage {
        val headers = dto.payload?.headers.orEmpty().associate { it.name.lowercase() to it.value }
        val fromHeader = headers["from"].orEmpty()
        val subject = headers["subject"] ?: "(no subject)"
        val sender = extractEmailAddress(fromHeader)
        val senderName = extractDisplayName(fromHeader)
        val body = dto.payload?.let { extractPlainText(it) }.orEmpty()
        val receivedAt = dto.internalDate?.toLongOrNull() ?: System.currentTimeMillis()
        val isUnread = dto.labelIds.contains("UNREAD")

        return EmailMessage(
            id = dto.id,
            threadId = dto.threadId,
            accountId = accountId,
            sender = sender,
            senderName = senderName,
            subject = subject,
            snippet = dto.snippet,
            content = body.take(MAX_CONTENT_CHARS),
            receivedAt = receivedAt,
            isRead = !isUnread,
            isImportant = false, // set once AI insight is applied
            hasInsight = false,
        )
    }

    private fun extractPlainText(payload: GmailPayloadDto): String {
        // Depth-first search: prefer text/plain, fall back to text/html (tags stripped).
        val plain = findPart(payload, "text/plain")
        if (plain != null) return decodeBody(plain.body?.data)

        val html = findPart(payload, "text/html")
        if (html != null) return stripHtml(decodeBody(html.body?.data))

        return decodeBody(payload.body?.data)
    }

    private fun findPart(payload: GmailPayloadDto, mimeType: String): GmailPayloadDto? {
        if (payload.mimeType == mimeType && payload.body?.data != null) return payload
        for (part in payload.parts) {
            findPart(part, mimeType)?.let { return it }
        }
        return null
    }

    private fun decodeBody(data: String?): String {
        if (data.isNullOrBlank()) return ""
        return try {
            String(Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun extractEmailAddress(fromHeader: String): String {
        val match = Regex("<([^>]+)>").find(fromHeader)
        return match?.groupValues?.get(1) ?: fromHeader.trim()
    }

    private fun extractDisplayName(fromHeader: String): String? {
        val withoutEmail = fromHeader.substringBefore("<").trim().trim('"')
        return withoutEmail.ifBlank { null }
    }
}
