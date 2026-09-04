package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.local.dao.ExecutiveItemDao
import com.inialpha.executiveai.data.local.entity.ExecutiveItemEntity
import com.inialpha.executiveai.domain.model.ExecutiveItem
import com.inialpha.executiveai.domain.model.ExecutiveItemState
import com.inialpha.executiveai.domain.model.ExecutiveItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * The Accept / Edit / Reject / Complete workflow lives here. This is the single place a
 * PROPOSED item becomes ACCEPTED (a "commitment", per REQUIREMENTS.md) — nothing upstream
 * ever writes ACCEPTED directly. Downstream, [com.inialpha.executiveai.notification.ReminderScheduler]
 * and [CalendarRepository.createEvent] key off ACCEPTED state changes to actually execute
 * (schedule a notification / create a calendar event) — kept as a separate step so this
 * repository stays a pure state machine.
 */
class ExecutiveItemRepository(
    private val dao: ExecutiveItemDao,
) {
    fun observeAll(): Flow<List<ExecutiveItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeByType(type: ExecutiveItemType): Flow<List<ExecutiveItem>> =
        dao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    fun observeByState(state: ExecutiveItemState): Flow<List<ExecutiveItem>> =
        dao.observeByState(state.name).map { list -> list.map { it.toDomain() } }

    fun observeUpcomingAccepted(): Flow<List<ExecutiveItem>> =
        dao.observeUpcomingAccepted().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): ExecutiveItem? = dao.getById(id)?.toDomain()

    /** Voice-originated items (see [com.inialpha.executiveai.voice]) enter the same proposal pipeline. */
    suspend fun createVoiceProposal(accountId: String, title: String, description: String?): ExecutiveItem {
        val now = System.currentTimeMillis()
        val entity = ExecutiveItemEntity(
            id = UUID.randomUUID().toString(), sourceEmailId = null, sourceThreadId = null,
            accountId = accountId, type = ExecutiveItemType.VOICE_COMMAND.name, title = title,
            description = description, location = null, dueAtMillis = null,
            state = ExecutiveItemState.PROPOSED.name, createdAt = now, updatedAt = now, executionRef = null,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun accept(id: String): ExecutiveItem? = transition(id, ExecutiveItemState.ACCEPTED)

    suspend fun reject(id: String): ExecutiveItem? = transition(id, ExecutiveItemState.REJECTED)

    suspend fun complete(id: String): ExecutiveItem? = transition(id, ExecutiveItemState.COMPLETED)

    suspend fun edit(
        id: String,
        title: String? = null,
        description: String? = null,
        location: String? = null,
        dueAtMillis: Long? = null,
    ): ExecutiveItem? {
        val existing = dao.getById(id) ?: return null
        val updated = existing.copy(
            title = title ?: existing.title,
            description = description ?: existing.description,
            location = location ?: existing.location,
            dueAtMillis = dueAtMillis ?: existing.dueAtMillis,
            state = ExecutiveItemState.EDITED.name,
            updatedAt = System.currentTimeMillis(),
        )
        dao.update(updated)
        return updated.toDomain()
    }

    suspend fun markExecuted(id: String, executionRef: String) {
        dao.getById(id)?.let { dao.update(it.copy(executionRef = executionRef, updatedAt = System.currentTimeMillis())) }
    }

    private suspend fun transition(id: String, newState: ExecutiveItemState): ExecutiveItem? {
        val existing = dao.getById(id) ?: return null
        val updated = existing.copy(state = newState.name, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        return updated.toDomain()
    }
}

private fun ExecutiveItemEntity.toDomain() = ExecutiveItem(
    id = id, sourceEmailId = sourceEmailId, sourceThreadId = sourceThreadId, accountId = accountId,
    type = ExecutiveItemType.valueOf(type), title = title, description = description, location = location,
    dueAtMillis = dueAtMillis, state = ExecutiveItemState.valueOf(state), createdAt = createdAt,
    updatedAt = updatedAt, executionRef = executionRef,
)
