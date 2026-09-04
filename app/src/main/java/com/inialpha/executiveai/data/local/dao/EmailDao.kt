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

    @Query("SELECT * FROM emails WHERE hasInsight = 0 ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun getWithoutInsight(limit: Int): List<EmailEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(emails: List<EmailEntity>)

    @Query("UPDATE emails SET hasInsight = 1, isImportant = :isImportant WHERE id = :id")
    suspend fun markInsightApplied(id: String, isImportant: Boolean)

    @Query("DELETE FROM emails WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: String)
}
