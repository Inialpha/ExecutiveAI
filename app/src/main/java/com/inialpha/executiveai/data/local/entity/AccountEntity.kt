package com.inialpha.executiveai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isGmailSyncEnabled: Boolean,
    val isCalendarSyncEnabled: Boolean,
    val lastGmailSyncAt: Long?,
    val lastCalendarSyncAt: Long?,
    val addedAt: Long,
)
