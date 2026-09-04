package com.inialpha.executiveai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
    indices = [Index("accountId"), Index("startAtMillis")],
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val calendarId: String,
    val accountId: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val isAllDay: Boolean,
    val htmlLink: String?,
)
