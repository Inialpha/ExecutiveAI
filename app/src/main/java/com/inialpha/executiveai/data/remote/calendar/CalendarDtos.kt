package com.inialpha.executiveai.data.remote.calendar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEventListResponseDto(
    @SerialName("items") val items: List<CalendarEventDto> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null,
)

@Serializable
data class CalendarEventDto(
    @SerialName("id") val id: String,
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("start") val start: CalendarEventDateTimeDto? = null,
    @SerialName("end") val end: CalendarEventDateTimeDto? = null,
    @SerialName("htmlLink") val htmlLink: String? = null,
    @SerialName("status") val status: String? = null,
)

@Serializable
data class CalendarEventDateTimeDto(
    @SerialName("date") val date: String? = null,
    @SerialName("dateTime") val dateTime: String? = null,
    @SerialName("timeZone") val timeZone: String? = null,
)

/** Body for creating a new event once the user accepts a proposed EVENT item. */
@Serializable
data class CalendarEventCreateDto(
    @SerialName("summary") val summary: String,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("start") val start: CalendarEventDateTimeDto,
    @SerialName("end") val end: CalendarEventDateTimeDto,
)
