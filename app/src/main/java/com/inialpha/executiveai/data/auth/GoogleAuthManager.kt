package com.inialpha.executiveai.data.auth

import android.accounts.Account
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Scopes Executive AI ever requests, requested incrementally per REQUIREMENTS.md ("request only what's needed"). */
object AccountAuthScopes {
    const val EMAIL = "https://www.googleapis.com/auth/userinfo.email"
    const val PROFILE = "https://www.googleapis.com/auth/userinfo.profile"
    const val GMAIL_READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    const val CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
    const val CALENDAR_EVENTS = "https://www.googleapis.com/auth/calendar.events"

    /** Requested once, when a new account is first connected. */
    val BASE_IDENTITY = listOf(EMAIL, PROFILE)
}

sealed class AuthorizationOutcome {
    data class Success(val accessToken: String, val grantedScopes: List<String>) : AuthorizationOutcome()
    object Cancelled : AuthorizationOutcome()
    data class Failure(val message: String) : AuthorizationOutcome()
}

/**
 * Wraps the Identity Services `AuthorizationClient` — the current (non-deprecated) API for
 * granting an Android app access to Gmail/Calendar scopes for a Google account, per
 * https://developer.android.com/identity/authorization.
 *
 * This class deliberately never persists the resulting access token: every call to [authorize]
 * fetches (or silently re-confirms) a token on demand. Callers are responsible for using the
 * token immediately and discarding it.
 *
 * Must be constructed in `onCreate`, before the host [ComponentActivity] reaches STARTED, because
 * it registers an [ActivityResultLauncher].
 */
class GoogleAuthManager(private val activity: ComponentActivity) {

    private var pendingContinuation: kotlin.coroutines.Continuation<Intent?>? = null

    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            pendingContinuation?.resume(result.data)
            pendingContinuation = null
        }

    /**
     * Requests [scopes] for the signed-in Google account. If [accountEmail] is provided, targets
     * that specific device account (used to silently refresh a token for an already-connected
     * Executive AI account); if null, the system account chooser is shown, which is how a *new*
     * account gets connected.
     */
    suspend fun authorize(scopes: List<String>, accountEmail: String? = null): AuthorizationOutcome {
        val requestBuilder = AuthorizationRequest.builder()
            .setRequestedScopes(scopes.map { Scope(it) })
        if (accountEmail != null) {
            requestBuilder.setAccount(Account(accountEmail, "com.google"))
        }
        val request = requestBuilder.build()

        val initialResult = try {
            suspendCancellableCoroutine<AuthorizationResult> { cont ->
                Identity.getAuthorizationClient(activity)
                    .authorize(request)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
        } catch (e: Exception) {
            return AuthorizationOutcome.Failure(e.message ?: "Authorization request failed")
        }

        val finalResult: AuthorizationResult = if (initialResult.hasResolution()) {
            val pendingIntent = initialResult.pendingIntent
                ?: return AuthorizationOutcome.Failure("Missing consent screen intent")
            val resultIntent = suspendCancellableCoroutine<Intent?> { cont ->
                pendingContinuation = cont
                launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            } ?: return AuthorizationOutcome.Cancelled

            try {
                Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(resultIntent)
            } catch (e: ApiException) {
                return AuthorizationOutcome.Failure(e.message ?: "User did not grant access")
            }
        } else {
            initialResult
        }

        val token = finalResult.accessToken
            ?: return AuthorizationOutcome.Failure("Google did not return an access token")
        // AuthorizationResult.grantedScopes is already a non-nullable List<String> of scope URIs.
        val granted = finalResult.grantedScopes
        return AuthorizationOutcome.Success(token, granted)
    }

    /**
     * Best-effort local revocation. AuthorizationClient has no direct `revokeAccess()` in the
     * current Identity Services API surface, so this is intentionally a no-op stub rather than a
     * guessed method call — the user can always revoke access from their Google Account's
     * "Third-party apps & services" settings, which is the reliable path. Wire this up to a real
     * revocation call (if/when one is confirmed against the exact play-services-auth version in
     * use) rather than reintroducing an unverified API guess.
     */
    fun revokeLocalToken() {
        // Intentionally empty — see doc comment above.
    }
}
