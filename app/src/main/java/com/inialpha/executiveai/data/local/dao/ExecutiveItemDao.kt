package com.inialpha.executiveai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inialpha.executiveai.data.local.entity.ExecutiveItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutiveItemDao {
    @Query("SELECT * FROM executive_items ORDER BY COALESCE(dueAtMillis, createdAt) ASC")
    fun observeAll(): Flow<List<ExecutiveItemEntity>>

    @Query("SELECT * FROM executive_items WHERE type = :type ORDER BY COALESCE(dueAtMillis, createdAt) ASC")
    fun observeByType(type: String): Flow<List<ExecutiveItemEntity>>

    @Query("SELECT * FROM executive_items WHERE state = :state ORDER BY COALESCE(dueAtMillis, createdAt) ASC")
    fun observeByState(state: String): Flow<List<ExecutiveItemEntity>>

    @Query(
        "SELECT * FROM executive_items WHERE state IN ('ACCEPTED') AND dueAtMillis IS NOT NULL " +
            "ORDER BY dueAtMillis ASC"
    )
    fun observeUpcomingAccepted(): Flow<List<ExecutiveItemEntity>>

    @Query("SELECT * FROM executive_items WHERE id = :id")
    suspend fun getById(id: String): ExecutiveItemEntity?

    @Query("SELECT * FROM executive_items WHERE sourceEmailId = :emailId")
    suspend fun getForEmail(emailId: String): List<ExecutiveItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ExecutiveItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExecutiveItemEntity>)

    @Update
    suspend fun update(item: ExecutiveItemEntity)

    @Query("DELETE FROM executive_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
