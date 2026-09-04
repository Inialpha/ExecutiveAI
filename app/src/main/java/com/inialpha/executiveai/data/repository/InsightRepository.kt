package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.local.InsightJson
import com.inialpha.executiveai.data.local.dao.EmailDao
import com.inialpha.executiveai.data.local.dao.ExecutiveItemDao
import com.inialpha.executiveai.data.local.dao.InsightDao
import com.inialpha.executiveai.data.local.entity.ExecutiveItemEntity
import com.inialpha.executiveai.data.local.entity.InsightEntity
import com.inialpha.executiveai.data.remote.NetworkFactory
import com.inialpha.executiveai.data.remote.ai.AiInsightApi
import com.inialpha.executiveai.data.remote.ai.EmailPayloadDto
import com.inialpha.executiveai.data.remote.ai.InsightRequestDto
import com.inialpha.executiveai.data.remote.ai.InsightResponseDto
import com.inialpha.executiveai.domain.model.EmailInsight
import com.inialpha.executiveai.domain.model.EmailMessage
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

sealed class InsightFetchResult {
    data class Success(val processedCount: Int, val skippedCount: Int) : InsightFetchResult()
    object Empty : InsightFetchResult()
    object AuthOrServerUnavailable : InsightFetchResult()
    data class NetworkError(val message: String) : InsightFetchResult()
    data class ApiError(val code: Int, val message: String) : InsightFetchResult()
}

/**
 * Bridges Android to the EmailManager AI gateway. Sends the full batch of emails needing
 * insight in one request (no client-side chunking, per REQUIREMENTS.md), and turns each
 * successfully-processed result into:
 *  1. a persisted [InsightEntity] (for the Email Insight screen), and
 *  2. one PROPOSED [ExecutiveItemEntity] per event/action/deadline/reminder the AI found — never
 *     ACCEPTED automatically. The user reviews and decides from there.
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
     * Fetches insights for up to [maxBatch] emails that don't have one yet. Individual malformed
     * results from the backend are skipped rather than failing the whole batch.
     */
    suspend fun fetchInsightsForPendingEmails(maxBatch: Int = 25): InsightFetchResult {
        val pending = emailDao.getWithoutInsight(maxBatch)
        if (pending.isEmpty()) return InsightFetchResult.Empty

        val request = InsightRequestDto(
            currentDatetime = currentIsoDatetimeWithOffset(),
            emails = pending.map {
                EmailPayloadDto(
                    id = it.id,
                    threadId = it.threadId,
                    sender = it.sender,
                    subject = it.subject,
                    content = it.content,
                    snippet = it.snippet,
                )
            },
        )

        return try {
            val response = api.extractInsights(request)
            if (!response.isSuccessful) {
                return if (response.code() in intArrayOf(401, 403, 502, 503, 504)) {
                    InsightFetchResult.AuthOrServerUnavailable
                } else {
                    InsightFetchResult.ApiError(response.code(), response.message())
                }
            }

            val results = response.body().orEmpty()
            val accountByEmailId = pending.associateBy({ it.id }, { it.accountId })
            var processed = 0
            for (dto in results) {
                val accountId = accountByEmailId[dto.id] ?: continue // result we can't associate with a synced email: skip
                persistInsight(accountId, dto)
                processed++
            }
            InsightFetchResult.Success(processedCount = processed, skippedCount = pending.size - processed)
        } catch (e: IOException) {
            InsightFetchResult.NetworkError(e.message ?: "Network error contacting the AI gateway")
        } catch (e: HttpException) {
            InsightFetchResult.ApiError(e.code(), e.message())
        }
    }

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
        emailDao.markInsightApplied(dto.id, dto.isImportant)

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
