package com.inialpha.executiveai.data.local

import com.inialpha.executiveai.domain.model.InsightAction
import com.inialpha.executiveai.domain.model.InsightDeadline
import com.inialpha.executiveai.domain.model.InsightEvent
import com.inialpha.executiveai.domain.model.InsightReminder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Not a Room @TypeConverter class itself (each JSON list is a plain String column on
 * [com.inialpha.executiveai.data.local.entity.InsightEntity]) — these are the shared
 * encode/decode helpers used by the insight mapping layer so the JSON shape lives in one place.
 */
object InsightJson {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encodeEvents(list: List<InsightEvent>): String = json.encodeToString(list)
    fun decodeEvents(raw: String): List<InsightEvent> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)

    fun encodeActions(list: List<InsightAction>): String = json.encodeToString(list)
    fun decodeActions(raw: String): List<InsightAction> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)

    fun encodeDeadlines(list: List<InsightDeadline>): String = json.encodeToString(list)
    fun decodeDeadlines(raw: String): List<InsightDeadline> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)

    fun encodeReminders(list: List<InsightReminder>): String = json.encodeToString(list)
    fun decodeReminders(raw: String): List<InsightReminder> =
        if (raw.isBlank()) emptyList() else json.decodeFromString(raw)
}
