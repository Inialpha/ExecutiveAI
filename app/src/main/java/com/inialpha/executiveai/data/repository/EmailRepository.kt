package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.local.dao.EmailDao
import com.inialpha.executiveai.data.local.entity.EmailEntity
import com.inialpha.executiveai.data.remote.NetworkFactory
import com.inialpha.executiveai.data.remote.gmail.GmailApi
import com.inialpha.executiveai.data.remote.gmail.GmailMessageMapper
import com.inialpha.executiveai.domain.model.EmailMessage
import com.inialpha.executiveai.domain.model.EmailProcessingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

sealed class SyncResult {
    data class Success(val newMessageCount: Int) : SyncResult()
    object AuthExpired : SyncResult()
    data class NetworkError(val message: String) : SyncResult()
    data class ApiError(val code: Int, val message: String) : SyncResult()
}

/**
 * Owns Gmail retrieval + local persistence. Android is the sole owner of this data — nothing
 * here ever touches the EmailManager backend, which only receives already-synced email content
 * for AI processing (see [InsightRepository]).
 *
 * This repository is deliberately unaware of AI processing: [syncAccount] only gathers and
 * persists Gmail messages (requirement: "synchronization must be incremental" / not dependent on
 * processing succeeding). [InsightRepository] is a separate step, run afterwards, that consumes
 * whatever is left in PENDING/FAILED state.
 */
class EmailRepository(
    private val emailDao: EmailDao,
) {
    private val gmailApi: GmailApi = NetworkFactory.retrofit(GmailApi.BASE_URL).create(GmailApi::class.java)

    fun observeAll(): Flow<List<EmailMessage>> = emailDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeForAccount(accountId: String): Flow<List<EmailMessage>> =
        emailDao.observeForAccount(accountId).map { list -> list.map { it.toDomain() } }

    fun observeImportant(): Flow<List<EmailMessage>> =
        emailDao.observeImportant().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): EmailMessage? = emailDao.getById(id)?.toDomain()

    /**
     * Synchronizes the most recent inbox messages for one connected account. Handles empty
     * inboxes, expired authorization (401/403), network failures, and malformed individual
     * messages (skipped, not fatal to the whole sync) per REQUIREMENTS.md section 6.
     *
     * Crucially: an email that already exists locally keeps its existing `processingStatus` (and
     * `isImportant`) — re-syncing never resets a COMPLETED or FAILED email back to PENDING. Only
     * genuinely new messages are inserted as PENDING.
     */
    suspend fun syncAccount(accessToken: String, accountId: String, maxResults: Int = 25): SyncResult {
        val bearer = "Bearer $accessToken"
        return try {
            val listResponse = gmailApi.listMessages(bearer, maxResults = maxResults)
            if (!listResponse.isSuccessful) {
                return listResponse.toAuthOrApiError()
            }
            val refs = listResponse.body()?.messages.orEmpty()
            if (refs.isEmpty()) {
                return SyncResult.Success(0) // empty inbox is a valid, non-error state
            }

            val existingById = emailDao.getByIds(refs.map { it.id }).associateBy { it.id }

            val messages = mutableListOf<EmailEntity>()
            for (ref in refs) {
                val existing = existingById[ref.id]
                if (existing != null) {
                    // Already known locally: never re-fetch or reset its processing state.
                    continue
                }
                val detail = runCatching { gmailApi.getMessage(bearer, ref.id) }.getOrNull()
                val body = detail?.takeIf { it.isSuccessful }?.body() ?: continue // skip malformed/failed individual messages
                messages += GmailMessageMapper.toDomain(accountId, body).toEntity()
            }

            if (messages.isNotEmpty()) emailDao.upsertAll(messages)
            SyncResult.Success(messages.size)
        } catch (e: IOException) {
            SyncResult.NetworkError(e.message ?: "Network error while syncing Gmail")
        } catch (e: HttpException) {
            SyncResult.ApiError(e.code(), e.message())
        }
    }

    suspend fun deleteForAccount(accountId: String) = emailDao.deleteForAccount(accountId)
}

private fun <T> retrofit2.Response<T>.toAuthOrApiError(): SyncResult =
    if (code() == 401 || code() == 403) SyncResult.AuthExpired
    else SyncResult.ApiError(code(), message())

private fun EmailEntity.toDomain() = EmailMessage(
    id = id, threadId = threadId, accountId = accountId, sender = sender, senderName = senderName,
    subject = subject, snippet = snippet, content = content, receivedAt = receivedAt,
    isRead = isRead, isImportant = isImportant,
    processingStatus = runCatching { EmailProcessingStatus.valueOf(processingStatus) }.getOrDefault(EmailProcessingStatus.PENDING),
)

private fun EmailMessage.toEntity() = EmailEntity(
    id = id, threadId = threadId, accountId = accountId, sender = sender, senderName = senderName,
    subject = subject, snippet = snippet, content = content, receivedAt = receivedAt,
    isRead = isRead, isImportant = isImportant, processingStatus = processingStatus.name,
)
