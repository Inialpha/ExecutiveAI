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
     * Emails still needing AI processing for one account — PENDING (never sent) or FAILED
     * (attempted, didn't complete), oldest received first. COMPLETED emails are never returned
     * here, so a later synchronization naturally skips them. See
     * [com.inialpha.executiveai.data.repository.InsightRepository] for the sequential,
     * one-at-a-time consumer of this list.
     */
    @Query(
        "SELECT * FROM emails WHERE accountId = :accountId AND processingStatus != 'COMPLETED' " +
            "ORDER BY receivedAt ASC"
    )
    suspend fun getUnprocessedForAccount(accountId: String): List<EmailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(emails: List<EmailEntity>)

    @Query("UPDATE emails SET processingStatus = :status, isImportant = :isImportant WHERE id = :id")
    suspend fun updateProcessingResult(id: String, status: String, isImportant: Boolean)

    @Query("UPDATE emails SET processingStatus = :status WHERE id = :id")
    suspend fun updateProcessingStatus(id: String, status: String)

    @Query("DELETE FROM emails WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
