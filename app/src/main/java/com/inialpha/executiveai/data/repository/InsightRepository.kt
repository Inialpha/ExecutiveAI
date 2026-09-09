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
import com.inialpha.executiveai.data.remote.ai.InsightBatchResponseDto
import com.inialpha.executiveai.data.remote.ai.InsightRequestDto
import com.inialpha.executiveai.data.remote.ai.InsightResponseDto
import com.inialpha.executiveai.domain.model.EmailInsight
import com.inialpha.executiveai.domain.model.EmailProcessingStatus
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Bridges Android to the EmailManager AI gateway — **one email at a time**, oldest received
 * first, per REQUIREMENTS.md's sequential-processing flow. The next email is never sent until
 * the current one's result has been validated and persisted (or marked FAILED), and every
 * outcome is written to Room immediately as it happens — not batched at the end — so a
 * successful result is visible in the rest of the app (e.g. the Emails screen) right away, and
 * an interruption or a single failure never loses or rolls back what already succeeded.
 *
 * Every stage of a single email's journey — request built, request sent, HTTP response, raw
 * body, parse attempt, validation, persistence — is traced through [EmailProcessingProgress] /
 * [EmailProcessingDebugInfo] when [EmailDebugConfig.ENABLED], specifically so a "backend
 * succeeded but Android recorded a failure" mismatch can be pinpointed to an exact stage instead
 * of collapsing into one opaque FAILED. See EmailDebugConfig's doc comment for how to turn this
 * off later.
 */
class InsightRepository(
    private val emailDao: EmailDao,
    private val insightDao: InsightDao,
    private val executiveItemDao: ExecutiveItemDao,
) {
    private val api: AiInsightApi = NetworkFactory.retrofit(AiInsightApi.BASE_URL).create(AiInsightApi::class.java)

    /** Used only for debug-trace (de)serialization here — separate from NetworkFactory's converter
     * so a parse failure can be caught and inspected directly, with the raw text preserved. */
    private val debugJson = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    fun observeAll(): Flow<List<EmailInsight>> = insightDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getForEmail(emailId: String): EmailInsight? = insightDao.getByEmailId(emailId)?.toDomain()

    /**
     * Processes every PENDING/FAILED email for [accountId] received at or after [sinceMillis]
     * (the currently selected sync window), strictly one at a time, oldest received first —
     * awaiting each result before starting the next, and emitting an [EmailProcessingProgress]
     * event at every stage so the UI can show real progress (and, in debug builds, the full
     * request/response/parse trace) instead of a generic spinner. A per-email failure marks that
     * email FAILED and continues to the next one rather than aborting the whole run, so a single
     * bad email can't block everything behind it; it simply remains retryable (if still within
     * the window) on the next synchronization. Safe to call repeatedly / resume after an
     * interruption: it always just re-reads whatever is left in PENDING/FAILED state within the
     * window.
     */
    fun processAllPendingForAccount(accountId: String, accountLabel: String, sinceMillis: Long): Flow<EmailProcessingProgress> = flow {
        val queue = emailDao.getUnprocessedForAccount(accountId, sinceMillis)
        var succeeded = 0
        var failed = 0
        emit(EmailProcessingProgress(queue.size, 0, "", EmailProcessingPhase.STARTED, 0, 0))

        for (index in queue.indices) {
            val email = queue[index]
            val position = index + 1
            val label = emailLabel(email)
            var debug = if (EmailDebugConfig.ENABLED) EmailProcessingDebugInfo(emailId = email.id, accountLabel = accountLabel) else null

            suspend fun report(phase: EmailProcessingPhase) {
                emit(EmailProcessingProgress(queue.size, position, label, phase, succeeded, failed, debug))
            }

            // --- 1. Request prepared ---
            report(EmailProcessingPhase.STARTED)
            val request = InsightRequestDto(
                currentDatetime = currentIsoDatetimeWithOffset(),
                emails = listOf(
                    EmailPayloadDto(
                        id = email.id, threadId = email.threadId, sender = email.sender,
                        subject = email.subject, content = email.content, snippet = email.snippet,
                    ),
                ),
            )
            if (debug != null) {
                debug = debug.copy(requestBodyJson = debugRequestJson(request))
            }
            report(EmailProcessingPhase.REQUEST_PREPARED)

            // --- 2. Request sent / HTTP response received ---
            val response = try {
                api.extractInsightsRaw(request)
            } catch (e: IOException) {
                debug = debug?.copy(failureStage = FailureStage.REQUEST_SEND, failureReason = e.message ?: "Network error contacting the AI gateway")
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            } catch (e: Exception) {
                debug = debug?.copy(failureStage = FailureStage.REQUEST_SEND, failureReason = e.message ?: "Unexpected error sending the request")
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            }
            report(EmailProcessingPhase.REQUEST_SENT)

            val httpStatus = response.code()
            val httpSuccess = response.isSuccessful
            val bodyText = try {
                if (httpSuccess) response.body()?.string() else response.errorBody()?.string()
            } catch (e: IOException) {
                null
            }
            debug = debug?.copy(httpStatusCode = httpStatus, httpSuccess = httpSuccess, rawResponseBody = bodyText?.take(EmailDebugConfig.MAX_TEXT_LENGTH))
            report(EmailProcessingPhase.RESPONSE_RECEIVED)

            if (!httpSuccess) {
                debug = debug?.copy(failureStage = FailureStage.HTTP_ERROR, failureReason = "HTTP $httpStatus")
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            }

            // --- 3. Response parsed ---
            report(EmailProcessingPhase.PARSING_RESPONSE)
            if (bodyText.isNullOrBlank()) {
                debug = debug?.copy(failureStage = FailureStage.RESPONSE_PARSING, failureReason = "Response body was empty")
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            }

            val parsedResults: List<InsightResponseDto>? = parseResponseBody(bodyText)
            if (parsedResults == null) {
                val errorMessage = lastParseError(bodyText)
                debug = debug?.copy(failureStage = FailureStage.RESPONSE_PARSING, parseErrorMessage = errorMessage, failureReason = errorMessage)
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            }

            // --- 4. Success/failure determined (schema validation: does a result for THIS email exist?) ---
            val result = parsedResults.firstOrNull { it.id == email.id } ?: parsedResults.singleOrNull()
            if (result == null) {
                debug = debug?.copy(
                    failureStage = FailureStage.SCHEMA_VALIDATION,
                    failureReason = "Parsed successfully, but no result matched email id ${email.id} (parsed ${parsedResults.size} result(s))",
                )
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            }
            debug = debug?.copy(parsedResultJson = debugJson.encodeToString(InsightResponseDto.serializer(), result))

            // --- 5. Result saved ---
            report(EmailProcessingPhase.SAVING_RESULT)
            try {
                persistInsight(accountId, email, result)
                emailDao.updateProcessingResult(email.id, EmailProcessingStatus.COMPLETED.name, result.isImportant)
            } catch (e: Exception) {
                debug = debug?.copy(failureStage = FailureStage.LOCAL_PERSISTENCE, failureReason = e.message ?: "Failed to save the result locally")
                emailDao.updateProcessingStatus(email.id, EmailProcessingStatus.FAILED.name)
                failed++
                report(EmailProcessingPhase.FAILED)
                continue
            }

            succeeded++
            report(EmailProcessingPhase.SUCCEEDED)
        }

        emit(EmailProcessingProgress(queue.size, queue.size, "", EmailProcessingPhase.COMPLETE, succeeded, failed))
    }

    /** Tries the documented single-object shape first, then falls back to a JSON array, since the exact
     * wire shape for a one-email request was unconfirmed at the time this fallback was written — see
     * ARCHITECTURE.md. Returns null (never throws) if neither shape parses. */
    /** Tries the confirmed real shape first — `{"emails": [...]}` — then falls back to a bare
     * array or a bare single object in case the backend's exact envelope varies by code path.
     * Returns null (never throws) only if none of the three shapes parse. */
    private fun parseResponseBody(bodyText: String): List<InsightResponseDto>? {
        runCatching { return debugJson.decodeFromString(InsightBatchResponseDto.serializer(), bodyText).emails }
        runCatching { return debugJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(InsightResponseDto.serializer()), bodyText) }
        runCatching { return listOf(debugJson.decodeFromString(InsightResponseDto.serializer(), bodyText)) }
        return null
    }

    private fun lastParseError(bodyText: String): String {
        val batchAttempt = runCatching { debugJson.decodeFromString(InsightBatchResponseDto.serializer(), bodyText) }
        val listAttempt = runCatching { debugJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(InsightResponseDto.serializer()), bodyText) }
        val singleAttempt = runCatching { debugJson.decodeFromString(InsightResponseDto.serializer(), bodyText) }
        return "As {\"emails\": [...]}: ${batchAttempt.exceptionOrNull()?.message}\n" +
            "As array: ${listAttempt.exceptionOrNull()?.message}\n" +
            "As single object: ${singleAttempt.exceptionOrNull()?.message}"
    }

    private fun debugRequestJson(request: InsightRequestDto): String {
        // Redact/truncate email body content in the debug trace only — the real request sent over
        // the wire (above) is unaffected. Per REQUIREMENTS: "be careful with sensitive information".
        val redacted = request.copy(emails = request.emails.map { it.copy(content = it.content.take(300) + if (it.content.length > 300) "…[truncated for debug]" else "") })
        return debugJson.encodeToString(InsightRequestDto.serializer(), redacted).take(EmailDebugConfig.MAX_TEXT_LENGTH)
    }

    private fun emailLabel(email: EmailEntity): String {
        val sender = email.senderName?.takeIf { it.isNotBlank() } ?: email.sender
        return "$sender — ${email.subject}"
    }

    /** Persists the insight and creates one PROPOSED [ExecutiveItemEntity] per extracted item.
     * Falls back to the locally-known, always-reliable Gmail [email] for threadId/sender when the
     * AI response omits them (both are nullable on the backend — see InsightResponseDto). */
    private suspend fun persistInsight(accountId: String, email: EmailEntity, dto: InsightResponseDto) {
        val now = System.currentTimeMillis()
        val entity = InsightEntity(
            emailId = dto.id,
            threadId = dto.threadId ?: email.threadId,
            accountId = accountId,
            sender = dto.sender ?: email.sender,
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
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId ?: email.threadId,
                accountId = accountId, type = ExecutiveItemType.EVENT.name, title = event.title,
                description = event.description, location = event.location,
                dueAtMillis = parseDateAndTime(event.date, event.time),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        dto.actions.forEach { action ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId ?: email.threadId,
                accountId = accountId, type = ExecutiveItemType.TASK.name, title = action.title,
                description = action.description, location = null,
                dueAtMillis = parseDateAndTime(action.dueDate, null),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        dto.deadlines.forEach { deadline ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId ?: email.threadId,
                accountId = accountId, type = ExecutiveItemType.DEADLINE.name, title = deadline.title,
                description = deadline.description, location = null,
                dueAtMillis = parseDateAndTime(deadline.date, null),
                state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
            )
        }
        dto.reminders.forEach { reminder ->
            proposedItems += ExecutiveItemEntity(
                id = UUID.randomUUID().toString(), sourceEmailId = dto.id, sourceThreadId = dto.threadId ?: email.threadId,
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
