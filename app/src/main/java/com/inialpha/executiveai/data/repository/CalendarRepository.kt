package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.local.dao.CalendarEventDao
import com.inialpha.executiveai.data.local.entity.CalendarEventEntity
import com.inialpha.executiveai.data.remote.NetworkFactory
import com.inialpha.executiveai.data.remote.calendar.CalendarApi
import com.inialpha.executiveai.data.remote.calendar.CalendarEventCreateDto
import com.inialpha.executiveai.data.remote.calendar.CalendarEventDateTimeDto
import com.inialpha.executiveai.data.remote.calendar.CalendarEventDto
import com.inialpha.executiveai.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class CalendarSyncResult {
    data class Success(val eventCount: Int) : CalendarSyncResult()
    object AuthExpired : CalendarSyncResult()
    data class NetworkError(val message: String) : CalendarSyncResult()
    data class ApiError(val code: Int, val message: String) : CalendarSyncResult()
}

sealed class CalendarWriteResult {
    data class Success(val eventId: String, val htmlLink: String?) : CalendarWriteResult()
    data class Failure(val message: String) : CalendarWriteResult()
}

/**
 * Calendar synchronization + full event CRUD — the single data source backing the Calendar
 * screen, which is now the app's one place for "upcoming events, calendar view, add/edit/delete,
 * AI-generated events, and sync" (see REQUIREMENTS change request section 9).
 *
 * Extensibility for future providers: every public method here already takes only
 * account-scoped, provider-agnostic inputs (an access token + plain event fields) and returns
 * provider-agnostic domain types — none of the Google-specific request/response shapes leak past
 * this class. Each persisted [CalendarEvent] carries a [CalendarEvent.provider] tag (currently
 * always [PROVIDER_GOOGLE]). Adding a second provider later means adding a sibling
 * `syncAccount`/`createEvent`/etc. implementation behind the same method signatures (or an
 * interface extracted from this class) and tagging its events with a new provider constant — not
 * redesigning this class or the Calendar UI. Not implemented now, per the change request.
 */
class CalendarRepository(
    private val calendarEventDao: CalendarEventDao,
) {
    private val api: CalendarApi = NetworkFactory.retrofit(CalendarApi.BASE_URL).create(CalendarApi::class.java)

    fun observeUpcoming(): Flow<List<CalendarEvent>> =
        calendarEventDao.observeFrom(System.currentTimeMillis()).map { list -> list.map { it.toDomain() } }

    suspend fun syncAccount(accessToken: String, accountId: String): CalendarSyncResult {
        val bearer = "Bearer $accessToken"
        val timeMin = Instant.now().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return try {
            val response = api.listEvents(bearer, timeMin = timeMin)
            if (!response.isSuccessful) {
                return if (response.code() == 401 || response.code() == 403) CalendarSyncResult.AuthExpired
                else CalendarSyncResult.ApiError(response.code(), response.message())
            }
            val items = response.body()?.items.orEmpty().filter { it.status != "cancelled" }
            val entities = items.mapNotNull { it.toEntity(accountId) }
            calendarEventDao.upsertAll(entities)
            CalendarSyncResult.Success(entities.size)
        } catch (e: IOException) {
            CalendarSyncResult.NetworkError(e.message ?: "Network error while syncing Calendar")
        } catch (e: HttpException) {
            CalendarSyncResult.ApiError(e.code(), e.message())
        }
    }

    /** Creates a new event. Called both from "Add event" and from accepting a proposed EVENT item — never automatically. */
    suspend fun createEvent(
        accessToken: String,
        accountId: String,
        title: String,
        description: String?,
        location: String?,
        startMillis: Long,
        endMillis: Long,
    ): CalendarWriteResult {
        val body = toCreateDto(title, description, location, startMillis, endMillis)
        return try {
            val response = api.createEvent(bearerToken = "Bearer $accessToken", event = body)
            val created = response.body()
            if (response.isSuccessful && created != null) {
                calendarEventDao.upsertAll(listOf(created.toEntity(accountId) ?: return CalendarWriteResult.Failure("Created event had no valid start time")))
                CalendarWriteResult.Success(created.id, created.htmlLink)
            } else {
                CalendarWriteResult.Failure("Calendar API returned ${response.code()}")
            }
        } catch (e: Exception) {
            CalendarWriteResult.Failure(e.message ?: "Failed to create calendar event")
        }
    }

    suspend fun updateEvent(
        accessToken: String,
        accountId: String,
        eventId: String,
        title: String,
        description: String?,
        location: String?,
        startMillis: Long,
        endMillis: Long,
    ): CalendarWriteResult {
        val body = toCreateDto(title, description, location, startMillis, endMillis)
        return try {
            val response = api.updateEvent(bearerToken = "Bearer $accessToken", eventId = eventId, event = body)
            val updated = response.body()
            if (response.isSuccessful && updated != null) {
                calendarEventDao.upsertAll(listOf(updated.toEntity(accountId) ?: return CalendarWriteResult.Failure("Updated event had no valid start time")))
                CalendarWriteResult.Success(updated.id, updated.htmlLink)
            } else {
                CalendarWriteResult.Failure("Calendar API returned ${response.code()}")
            }
        } catch (e: Exception) {
            CalendarWriteResult.Failure(e.message ?: "Failed to update calendar event")
        }
    }

    suspend fun deleteEvent(accessToken: String, eventId: String): CalendarWriteResult {
        return try {
            val response = api.deleteEvent(bearerToken = "Bearer $accessToken", eventId = eventId)
            if (response.isSuccessful) {
                calendarEventDao.deleteById(eventId)
                CalendarWriteResult.Success(eventId, null)
            } else {
                CalendarWriteResult.Failure("Calendar API returned ${response.code()}")
            }
        } catch (e: Exception) {
            CalendarWriteResult.Failure(e.message ?: "Failed to delete calendar event")
        }
    }

    suspend fun deleteForAccount(accountId: String) = calendarEventDao.deleteForAccount(accountId)

    private fun toCreateDto(
        title: String,
        description: String?,
        location: String?,
        startMillis: Long,
        endMillis: Long,
    ): CalendarEventCreateDto {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return CalendarEventCreateDto(
            summary = title, description = description, location = location,
            start = CalendarEventDateTimeDto(dateTime = start, timeZone = zone.id),
            end = CalendarEventDateTimeDto(dateTime = end, timeZone = zone.id),
        )
    }

    companion object {
        const val PROVIDER_GOOGLE = "google"
    }
}

private fun CalendarEventDto.toEntity(accountId: String): CalendarEventEntity? {
    val zone = ZoneId.systemDefault()
    val (startMillis, isAllDay) = start?.toEpochMillisAndAllDay(zone) ?: return null
    val endMillis = end?.toEpochMillisAndAllDay(zone)?.first ?: startMillis
    return CalendarEventEntity(
        id = id, calendarId = "primary", accountId = accountId, provider = CalendarRepository.PROVIDER_GOOGLE,
        title = summary ?: "(untitled event)", description = description, location = location,
        startAtMillis = startMillis, endAtMillis = endMillis, isAllDay = isAllDay, htmlLink = htmlLink,
    )
}

private fun com.inialpha.executiveai.data.remote.calendar.CalendarEventDateTimeDto.toEpochMillisAndAllDay(
    zone: ZoneId,
): Pair<Long, Boolean>? {
    dateTime?.let {
        return try {
            java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() to false
        } catch (e: Exception) {
            null
        }
    }
    date?.let {
        return try {
            LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() to true
        } catch (e: Exception) {
            null
        }
    }
    return null
}

private fun CalendarEventEntity.toDomain() = CalendarEvent(
    id = id, calendarId = calendarId, accountId = accountId, provider = provider, title = title,
    description = description, location = location, startAtMillis = startAtMillis, endAtMillis = endAtMillis,
    isAllDay = isAllDay, htmlLink = htmlLink,
)
