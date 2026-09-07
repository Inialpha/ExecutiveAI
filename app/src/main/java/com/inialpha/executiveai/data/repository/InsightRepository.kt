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
import kotlinx.coroutines.flow.flow
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

/** Outcome of processing a single email. */
private sealed class EmailOutcome {
    object Processed : EmailOutcome()
    data class Failed(val reason: String) : EmailOutcome()
}

/** Where a single email is in its processing lifecycle, for UI progress display. */
enum class EmailProcessingPhase {
    /** Emitted once at the start with the full queue size and nothing processed yet. */
    STARTED,
    /** About to send this email to the AI gateway. */
    PROCESSING,
    SUCCEEDED,
    FAILED,
    /** Emitted once at the end, after every email in the queue has been handled. */
    COMPLETE,
}

/**
 * One progress event for the currently running sequential batch — see
 * [InsightRepository.processAllPendingForAccount]. [currentIndex] is 1-based; 0 only for the
 * initial STARTED event. [succeededCount]/[failedCount] reflect everything completed so far,
 * *including* the item this event is reporting on for SUCCEEDED/FAILED phases.
 */
data class EmailProcessingProgress(
    val totalCount: Int,
    val currentIndex: Int,
    val currentEmailLabel: String,
    val phase: EmailProcessingPhase,
    val succeededCount: Int,
    val failedCount: Int,
)

/**
 * Bridges Android to the EmailManager AI gateway — **one email at a time**, oldest received
 * first, per REQUIREMENTS.md's sequential-processing flow. The next email is never sent until
 * the current one's result has been validated and persisted (or marked FAILED), and every
 * outcome is written to Room immediately as it happens — not batched at the end — so a
 * successful result is visible in the rest of the app (e.g. the Emails screen) right away, and
 * an interruption or a single failure never loses or rolls back what already succeeded.
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
     * Processes every PENDING/FAILED email for [accountId] received at or after [sinceMillis]
     * (the currently selected sync window), strictly one at a time, oldest received first —
     * awaiting each result before starting the next, and emitting an [EmailProcessingProgress]
     * event before and after each one so the UI can show real progress instead of a generic
     * spinner. A per-email failure marks that email FAILED and continues to the next one rather
     * than aborting the whole run — see [EmailProcessingPhase.FAILED] — so a single bad email
     * can't block everything behind it; it simply remains retryable (if still within the window)
     * on the next synchronization. Safe to call repeatedly / resume after an interruption: it
     * always just re-reads whatever is left in PENDING/FAILED state within the window.
     */
    fun processAllPendingForAccount(accountId: String, sinceMillis: Long): Flow<EmailProcessingProgress> = flow {
        val queue = emailDao.getUnprocessedForAccount(accountId, sinceMillis)
        var succeeded = 0
        var failed = 0
        emit(EmailProcessingProgress(queue.size, 0, "", EmailProcessingPhase.STARTED, 0, 0))

        queue.forEachIndexed { index, email ->
            val position = index + 1
            val label = emailLabel(email)
            emit(EmailProcessingProgress(queue.size, position, label, EmailProcessingPhase.PROCESSING, succeeded, failed))

            when (val outcome = processSingleEmail(accountId, email)) {
                EmailOutcome.Processed -> {
                    succeeded++
                    emit(EmailProcessingProgress(queue.size, position, label, EmailProcessingPhase.SUCCEEDED, succeeded, failed))
                }
                is EmailOutcome.Failed -> {
                    failed++
                    emit(EmailProcessingProgress(queue.size, position, label, EmailProcessingPhase.FAILED, succeeded, failed))
                }
            }
        }

        emit(EmailProcessingProgress(queue.size, queue.size, "", EmailProcessingPhase.COMPLETE, succeeded, failed))
    }

    private fun emailLabel(email: EmailEntity): String {
        val sender = email.senderName?.takeIf { it.isNotBlank() } ?: email.sender
        return "$sender — ${email.subject}"
    }

    /** Sends exactly one email to the AI gateway and persists the outcome (success or failure) immediately. */
    private suspend fun processSingleEmail(accountId: String, email: EmailEntity): EmailOutcome {
        return try {
            val request = InsightRequestDto(
                currentDatetime = currentIsoDatetimeWithOffset(),
                emails = listOf(
                    EmailPayloadDto(
                        id = email.id,
                        threadId = email.threadId,
                        sender = email.sender,
                        subject = email.subject,
                        content = email.content,
                        snippet = email.snippet,
                    ),
                ),
            )

            val response = api.extractInsights(request)
            if (!response.isSuccessful) {
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                return EmailOutcome.Failed("AI gateway returned HTTP ${response.code()}")
            }

            // Validate: the single result we asked for must actually be present and match this email.
            val result = response.body().orEmpty().firstOrNull { it.id == email.id }
            if (result == null) {
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                return EmailOutcome.Failed("AI gateway returned no result for this email")
            }

            persistInsight(accountId, result)
            emailDao.updateProcessingResult(email.id, EmailProcessingStatus.COMPLETED.name, result.isImportant)
            EmailOutcome.Processed
        } catch (e: IOException) {
            emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
            EmailOutcome.Failed(e.message ?: "Network error contacting the AI gateway")
        } catch (e: HttpException) {
            emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
            EmailOutcome.Failed(e.message())
        } catch (e: Exception) {
            // Any other unexpected failure (malformed response body, etc.) — mark FAILED and
            // retryable rather than losing track of this email or crashing the sync.
            emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
            EmailOutcome.Failed(e.message ?: "Unexpected error processing this email")
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
