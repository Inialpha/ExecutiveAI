package com.inialpha.executiveai.domain.model

/**
 * A connected Google account. Executive AI supports multiple simultaneous accounts
 * (e.g. personal@gmail.com, work@gmail.com). Each account is independently identifiable
 * by [id] (its Google account email is used as the stable id).
 *
 * IMPORTANT: this model intentionally never holds an OAuth access/refresh token.
 * Tokens are fetched on-demand from [com.inialpha.executiveai.data.auth.GoogleAuthManager]
 * and are never persisted to Room or sent to any backend.
 */
data class Account(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val isGmailSyncEnabled: Boolean,
    val isCalendarSyncEnabled: Boolean,
    val lastGmailSyncAt: Long?,
    val lastCalendarSyncAt: Long?,
    val addedAt: Long,
)
