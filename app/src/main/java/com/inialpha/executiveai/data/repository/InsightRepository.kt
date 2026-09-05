package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.local.InsightJson
import com.inialpha.executiveai.data.local.dao.EmailDao
import com.inialpha.executiveai.data.local.dao.ExecutiveItemDao
import com.inialpha.executiveai.data.local.dao.InsightDao
import com.inialpha.executiveai.data.local.entity.EmailEntity
import com.inialpha.executiveai.data.local.entity.ExecutiveItemEntity
import com.inialpha.executiveai.data.local.entity.InsightEntity
import com.inialpha.executiveai.data.remote.NetworkFactory
import com.inialpha.executiveai.data.remote.ai.AiInsightApi
import com.inialpha.executiveai.data.remote.ai.EmailPayloadDto
import com.inialpha.executiveai.data.remote.ai.InsightRequestDto
import com.inialpha.executiveai.data.remote.ai.InsightResponseDto
import com.inialpha.executiveai.domain.model.EmailInsight
import com.inialpha.executiveai.domain.model.EmailProcessingStatus
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Outcome of processing a single email — see [InsightRepository.processNextPendingEmail]. */
sealed class EmailProcessingResult {
    data class Processed(val emailId: String) : EmailProcessingResult()
    data class Failed(val emailId: String, val reason: String) : EmailProcessingResult()
    /** No PENDING/FAILED email remained for this account — the queue is empty. */
    object NothingToProcess : EmailProcessingResult()
}

/** Summary of a full sequential run across every unprocessed email for one account. */
data class SequentialProcessingSummary(
    val processedCount: Int,
    val failedCount: Int,
)

/**
 * Bridges Android to the EmailManager AI gateway — **one email at a time**, oldest received
 * first, per REQUIREMENTS.md's sequential-processing flow. The next email is never sent until
 * the current one's result has been validated and persisted (or marked FAILED), and every
 * outcome is written to Room immediately, so the queue position survives an app restart or a
 * mid-run interruption without any in-memory processing state.
 *
 * The AI gateway's request/response contract is unchanged from before — each call still POSTs
 * the same [InsightRequestDto] shape, just with a one-element `emails` list instead of a batch.
 */
class InsightRepository(
    private val emailDao: EmailDao,
    private val insightDao: InsightDao,
    private val executiveItemDao: ExecutiveItemDao,
) {
    private val api: AiInsightApi = NetworkFactory.retrofit(AiInsightApi.BASE_URL).create(AiInsightApi::class.java)

    fun observeAll(): Flow<List<EmailInsight>> = insightDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getForEmail(emailId: String): EmailInsight? = insightDao.getByEmailId(emailId)?.toDomain()

    /**
     * Processes every PENDING/FAILED email for [accountId], strictly one at a time, oldest
     * received first — awaiting each result before starting the next. A per-email failure marks
     * that email FAILED and continues to the next one rather than aborting the whole run, so a
     * single bad email can't block everything behind it (it simply remains retryable on the next
     * synchronization). Safe to call repeatedly / resume after an interruption: it always just
     * re-reads whatever is left in PENDING/FAILED state.
     */
    suspend fun processAllPendingForAccount(accountId: String): SequentialProcessingSummary {
        var processed = 0
        var failed = 0
        while (true) {
            when (val result = processNextPendingEmail(accountId)) {
                is EmailProcessingResult.Processed -> processed++
                is EmailProcessingResult.Failed -> failed++
                EmailProcessingResult.NothingToProcess -> return SequentialProcessingSummary(processed, failed)
            }
        }
    }

    /**
     * Processes exactly one email: the earliest-received PENDING/FAILED email for [accountId].
     * Exposed separately from [processAllPendingForAccount] so a caller (or a future
     * cancellable/observable UI) can process a single step at a time if needed — the sequential
     * loop above is just this called repeatedly.
     */
    suspend fun processNextPendingEmail(accountId: String): EmailProcessingResult {
        val next = emailDao.getUnprocessedForAccount(accountId).firstOrNull()
            ?: return EmailProcessingResult.NothingToProcess

        return try {
            val request = InsightRequestDto(
                currentDatetime = currentIsoDatetimeWithOffset(),
                emails = listOf(
                    EmailPayloadDto(
                        id = next.id,
                        threadId = next.threadId,
                        sender = next.sender,
                        subject = next.subject,
                        content = next.content,
                        snippet = next.snippet,
                    ),
                ),
            )

            val response = api.extractInsights(request)
            if (!response.isSuccessful) {
                emailDao.updateProcessingStatus(next.id, EmailProcessingStatus.FAILED.name)
                return EmailProcessingResult.Failed(next.id, "AI gateway returned HTTP ${response.code()}")
            }

            // Validate: the single result we asked for must actually be present and match this email.
            val result = response.body().orEmpty().firstOrNull { it.id == next.id }
            if (result == null) {
                emailDao.updateProcessingStatus(next.id, EmailProcessingStatus.FAILED.name)
                return EmailProcessingResult.Failed(next.id, "AI gateway returned no result for this email")
            }

            persistInsight(accountId, result)
            emailDao.updateProcessingResult(next.id, EmailProcessingStatus.COMPLETED.name, result.isImportant)
            EmailProcessingResult.Processed(next.id)
        } catch (e: IOException) {
            emailDao.updateProcessingStatus(next.id, EmailProcessingStatus.FAILED.name)
            EmailProcessingResult.Failed(next.id, e.message ?: "Network error contacting the AI gateway")
        } catch (e: HttpException) {
            emailDao.updateProcessingStatus(next.id, EmailProcessingStatus.FAILED.name)
            EmailProcessingResult.Failed(next.id, e.message())
        } catch (e: Exception) {
            // Any other unexpected failure (malformed response body, etc.) — mark FAILED and
            // retryable rather than losing track of this email or crashing the sync.
            emailDao.updateProcessingStatus(next.id, EmailProcessingStatus.FAILED.name)
            EmailProcessingResult.Failed(next.id, e.message ?: "Unexpected error processing this email")
        }
    }

    /** Persists the insight and creates one PROPOSED [ExecutiveItemEntity] per extracted item. */
    private suspend fun persistInsight(accountId: String, dto: InsightResponseDto) {
        val now = System.currentTimeMillis()
        val entity = InsightEntity(
            emailId = dto.id,
            threadId = dto.threadId,
            accountId = accountId,
            sender = dto.sender,
            subject = dto.subject,
            isImportant = dto.isImportant,
            summary = dto.summary,
            eventsJson = InsightJson.encodeEvents(dto.events.map {
                com.inialpha.executiveai.domain.model.InsightEvent(it.title, it.date, it.time, it.location, it.description)
            }),
            actionsJson = InsightJson.encodeActions(dto.actions.map {
                com.inialpha.executiveai.domain.model.InsightAction(it.title, it.description, it.dueDate)
            }),
            deadlinesJson = InsightJson.encodeDeadlines(dto.deadlines.map {
                com.inialpha.executiveai.domain.model.InsightDeadline(it.title, it.date, it.description)
            }),
            remindersJson = InsightJson.encodeReminders(dto.reminders.map {
                com.inialpha.executiveai.domain.model.InsightReminder(it.title, it.datetime, it.reason)
            }),
            fetchedAt = now,
        )
        insightDao.upsert(entity)

        val proposedItems = mutableListOf<ExecutiveItemEntity>()
        dto.events.forEach { event ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId,
                accountId = accountId, type = ExecutiveItemType.EVENT.name, title = event.title,
                description = event.description, location = event.location,
                dueAtMillis = parseDateAndTime(event.date, event.time),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        dto.actions.forEach { action ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId,
                accountId = accountId, type = ExecutiveItemType.TASK.name, title = action.title,
                description = action.description, location = null,
                dueAtMillis = parseDateAndTime(action.dueDate, null),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        dto.deadlines.forEach { deadline ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId,
                accountId = accountId, type = ExecutiveItemType.DEADLINE.name, title = deadline.title,
                description = deadline.description, location = null,
                dueAtMillis = parseDateAndTime(deadline.date, null),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        dto.reminders.forEach { reminder ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId,
                accountId = accountId, type = ExecutiveItemType.REMINDER.name, title = reminder.title,
                description = reminder.reason, location = null,
                dueAtMillis = parseIsoDatetime(reminder.datetime),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        if (proposedItems.isNotEmpty()) executiveItemDao.upsertAll(proposedItems)
    }

    private fun currentIsoDatetimeWithOffset(): String =
        OffsetDateTime.now(ZoneId.systemDefault()).withNano(0)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /** Best-effort parse of the AI's separate "date" (yyyy-MM-dd) + "time" (HH:mm) fields into epoch millis, local zone. */
    private fun parseDateAndTime(date: String?, time: String?): Long? {
        if (date.isNullOrBlank()) return null
        return try {
            val localDate = LocalDate.parse(date)
            val localTime = if (!time.isNullOrBlank()) LocalTime.parse(time) else LocalTime.MIDNIGHT
            LocalDateTime.of(localDate, localTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    /** Best-effort parse of a full ISO-8601 datetime (with or without offset) into epoch millis. */
    private fun parseIsoDatetime(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }
}

private fun InsightEntity.toDomain() = EmailInsight(
    emailId = emailId, threadId = threadId, accountId = accountId, sender = sender, subject = subject,
    isImportant = isImportant, summary = summary,
    events = InsightJson.decodeEvents(eventsJson),
    actions = InsightJson.decodeActions(actionsJson),
    deadlines = InsightJson.decodeDeadlines(deadlinesJson),
    reminders = InsightJson.decodeReminders(remindersJson),
    fetchedAt = fetchedAt,
)
