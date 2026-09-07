package com.inialpha.executiveai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inialpha.executiveai.data.local.entity.EmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE accountId = :accountId ORDER BY receivedAt DESC")
    fun observeForAccount(accountId: String): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE isImportant = 1 ORDER BY receivedAt DESC")
    fun observeImportant(): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE id = :id")
    suspend fun getById(id: String): EmailEntity?

    @Query("SELECT * FROM emails WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<EmailEntity>

    /**
     * Emails still needing AI processing for one account, **within the given synchronization
     * window** — PENDING (never sent) or FAILED (attempted, didn't complete) and received at or
     * after [sinceMillis], oldest received first. COMPLETED emails are never returned here, and a
     * FAILED email that has aged out of the currently selected sync window is not retried (per
     * REQUIREMENTS change request section 4's example: a 30-hour-old FAILED email is not eligible
     * under a 24-hour window). See [com.inialpha.executiveai.data.repository.InsightRepository]
     * for the sequential, one-at-a-time consumer of this list.
     */
    @Query(
        "SELECT * FROM emails WHERE accountId = :accountId AND processingStatus != 'COMPLETED' " +
            "AND receivedAt >= :sinceMillis ORDER BY receivedAt ASC"
    )
    suspend fun getUnprocessedForAccount(accountId: String, sinceMillis: Long): List<EmailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(emails: List<EmailEntity>)

    @Query("UPDATE emails SET processingStatus = :status, isImportant = :isImportant WHERE id = :id")
    suspend fun updateProcessingResult(id: String, status: String, isImportant: Boolean)

    @Query("UPDATE emails SET processingStatus = :status WHERE id = :id")
    suspend fun updateProcessingStatus(id: String, status: String)

    @Query("DELETE FROM emails WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
