package com.inialpha.executiveai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.inialpha.executiveai.data.local.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM email_insights ORDER BY fetchedAt DESC")
    fun observeAll(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM email_insights WHERE emailId = :emailId")
    suspend fun getByEmailId(emailId: String): InsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(insight: InsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(insights: List<InsightEntity>)
}
