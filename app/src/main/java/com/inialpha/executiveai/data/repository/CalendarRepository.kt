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

sealed class CreateEventResult {
    data class Success(val eventId: String, val htmlLink: String?) : CreateEventResult()
    data class Failure(val message: String) : CreateEventResult()
}

/**
 * Google Calendar synchronization + (future) event creation. Read-only sync today; write access
 * (creating an event from an accepted proposed [com.inialpha.executiveai.domain.model.ExecutiveItem])
 * is implemented but only invoked after explicit user acceptance — never automatically.
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

    /** Only called after the user accepts a proposed EVENT item. Never automatic. */
    suspend fun createEvent(
        accessToken: String,
        title: String,
        description: String?,
        location: String?,
        startMillis: Long,
        endMillis: Long,
    ): CreateEventResult {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val body = CalendarEventCreateDto(
            summary = title, description = description, location = location,
            start = CalendarEventDateTimeDto(dateTime = start, timeZone = zone.id),
            end = CalendarEventDateTimeDto(dateTime = end, timeZone = zone.id),
        )
        return try {
            val response = api.createEvent(bearerToken = "Bearer $accessToken", event = body)
            val created = response.body()
            if (response.isSuccessful && created != null) {
                CreateEventResult.Success(created.id, created.htmlLink)
            } else {
                CreateEventResult.Failure("Calendar API returned ${response.code()}")
            }
        } catch (e: Exception) {
            CreateEventResult.Failure(e.message ?: "Failed to create calendar event")
        }
    }

    suspend fun deleteForAccount(accountId: String) = calendarEventDao.deleteForAccount(accountId)
}

private fun CalendarEventDto.toEntity(accountId: String): CalendarEventEntity? {
    val zone = ZoneId.systemDefault()
    val (startMillis, isAllDay) = start?.toEpochMillisAndAllDay(zone) ?: return null
    val endMillis = end?.toEpochMillisAndAllDay(zone)?.first ?: startMillis
    return CalendarEventEntity(
        id = id, calendarId = "primary", accountId = accountId,
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
    id = id, calendarId = calendarId, accountId = accountId, title = title, description = description,
    location = location, startAtMillis = startAtMillis, endAtMillis = endAtMillis, isAllDay = isAllDay,
    htmlLink = htmlLink,
)
