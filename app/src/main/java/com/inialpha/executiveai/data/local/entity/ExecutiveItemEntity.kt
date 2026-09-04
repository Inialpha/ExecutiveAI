package com.inialpha.executiveai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "executive_items",
    indices = [Index("accountId"), Index("type"), Index("state"), Index("dueAtMillis")],
)
data class ExecutiveItemEntity(
    @PrimaryKey val id: String,
    val sourceEmailId: String?,
    val sourceThreadId: String?,
    val accountId: String,
    val type: String,
    val title: String,
    val description: String?,
    val location: String?,
    val dueAtMillis: Long?,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long,
    val executionRef: String?,
)
