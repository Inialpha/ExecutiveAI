package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.data.auth.AccountAuthScopes
import com.inialpha.executiveai.data.auth.AuthorizationOutcome
import com.inialpha.executiveai.data.auth.GoogleAuthManager
import com.inialpha.executiveai.data.local.dao.AccountDao
import com.inialpha.executiveai.data.local.entity.AccountEntity
import com.inialpha.executiveai.data.remote.NetworkFactory
import com.inialpha.executiveai.data.remote.google.UserInfoApi
import com.inialpha.executiveai.domain.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed class ConnectAccountResult {
    data class Success(val account: Account) : ConnectAccountResult()
    object Cancelled : ConnectAccountResult()
    data class Failure(val message: String) : ConnectAccountResult()
}

/**
 * Owns the connected-Google-account list. Every add/refresh flow goes through
 * [GoogleAuthManager], and only account *identity* (email, display name, photo) is ever
 * persisted — never tokens.
 */
class AccountRepository(
    private val accountDao: AccountDao,
) {
    private val userInfoApi: UserInfoApi =
        NetworkFactory.retrofit(UserInfoApi.BASE_URL).create(UserInfoApi::class.java)

    fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getAccount(id: String): Account? = accountDao.getById(id)?.toDomain()

    /** Step 1 of "Add Google account": show the account chooser + base identity consent. */
    suspend fun connectNewAccount(authManager: GoogleAuthManager): ConnectAccountResult {
        val outcome = authManager.authorize(AccountAuthScopes.BASE_IDENTITY)
        return when (outcome) {
            is AuthorizationOutcome.Success -> {
                val info = runCatching {
                    userInfoApi.getUserInfo("Bearer ${outcome.accessToken}")
                }.getOrNull()?.body()

                val email = info?.email
                    ?: return ConnectAccountResult.Failure("Could not resolve the Google account's email address")

                val existing = accountDao.getById(email)
                val entity = AccountEntity(
                    id = email,
                    email = email,
                    displayName = info.name ?: existing?.displayName,
                    photoUrl = info.picture ?: existing?.photoUrl,
                    isGmailSyncEnabled = existing?.isGmailSyncEnabled ?: true,
                    isCalendarSyncEnabled = existing?.isCalendarSyncEnabled ?: true,
                    lastGmailSyncAt = existing?.lastGmailSyncAt,
                    lastCalendarSyncAt = existing?.lastCalendarSyncAt,
                    addedAt = existing?.addedAt ?: System.currentTimeMillis(),
                )
                accountDao.upsert(entity)
                ConnectAccountResult.Success(entity.toDomain())
            }
            AuthorizationOutcome.Cancelled -> ConnectAccountResult.Cancelled
            is AuthorizationOutcome.Failure -> ConnectAccountResult.Failure(outcome.message)
        }
    }

    /** Grants Gmail read access for an already-connected account (called before first Gmail sync). */
    suspend fun ensureGmailAccess(authManager: GoogleAuthManager, accountEmail: String): AuthorizationOutcome =
        authManager.authorize(listOf(com.inialpha.executiveai.data.auth.AccountAuthScopes.GMAIL_READONLY), accountEmail)

    /** Grants Calendar read access for an already-connected account (called before first Calendar sync). */
    suspend fun ensureCalendarAccess(authManager: GoogleAuthManager, accountEmail: String): AuthorizationOutcome =
        authManager.authorize(listOf(com.inialpha.executiveai.data.auth.AccountAuthScopes.CALENDAR_READONLY), accountEmail)

    suspend fun setGmailSyncEnabled(accountId: String, enabled: Boolean) {
        accountDao.getById(accountId)?.let { accountDao.update(it.copy(isGmailSyncEnabled = enabled)) }
    }

    suspend fun setCalendarSyncEnabled(accountId: String, enabled: Boolean) {
        accountDao.getById(accountId)?.let { accountDao.update(it.copy(isCalendarSyncEnabled = enabled)) }
    }

    suspend fun markGmailSynced(accountId: String, atMillis: Long) {
        accountDao.getById(accountId)?.let { accountDao.update(it.copy(lastGmailSyncAt = atMillis)) }
    }

    suspend fun markCalendarSynced(accountId: String, atMillis: Long) {
        accountDao.getById(accountId)?.let { accountDao.update(it.copy(lastCalendarSyncAt = atMillis)) }
    }

    /** Disconnects an account: removes it and its locally synced data (see EmailRepository/CalendarRepository). */
    suspend fun disconnectAccount(accountId: String) {
        accountDao.deleteById(accountId)
    }
}

private fun AccountEntity.toDomain() = Account(
    id = id,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isGmailSyncEnabled = isGmailSyncEnabled,
    isCalendarSyncEnabled = isCalendarSyncEnabled,
    lastGmailSyncAt = lastGmailSyncAt,
    lastCalendarSyncAt = lastCalendarSyncAt,
    addedAt = addedAt,
)
