package com.inialpha.executiveai.data.repository

import com.inialpha.executiveai.BuildConfig

/**
 * Single on/off switch for the verbose request/response/parsing trace added to email processing
 * for diagnosing the "backend says success, Android records failure" issue. Tied to
 * [BuildConfig.DEBUG] so it's automatically off in release builds without needing anyone to
 * remember to flip a flag — flip [FORCE_DISABLED] to true to kill it even in debug builds once
 * the diagnosis is done, without removing the (now-inert) plumbing. Nothing here should ever be
 * treated as a permanent production feature — see REQUIREMENTS change request ("temporary
 * debugging") for the intended lifecycle of this file.
 */
object EmailDebugConfig {
    private const val FORCE_DISABLED = false
    val ENABLED: Boolean = BuildConfig.DEBUG && !FORCE_DISABLED

    /** Debug payloads/bodies are truncated to this length before being held in UI state or logged. */
    const val MAX_TEXT_LENGTH = 4000
}

/** Where a single email's processing definitively stopped — the direct answer to "why did this fail". */
enum class FailureStage {
    NONE,
    /** The HTTP request itself never got a response (network error, timeout, DNS, etc.). */
    REQUEST_SEND,
    /** A response came back, but with a non-2xx HTTP status. */
    HTTP_ERROR,
    /** HTTP 2xx, but the response body didn't deserialize into the expected schema. */
    RESPONSE_PARSING,
    /** Parsed successfully, but the result didn't correspond to the email that was sent (e.g. id mismatch, or absent from an array response). */
    SCHEMA_VALIDATION,
    /** Parsed and validated, but writing it to Room threw. */
    LOCAL_PERSISTENCE,
}

/** Finer-grained than [EmailProcessingPhase] alone — mirrors the flow the person asked to trace. */
enum class EmailProcessingPhase {
    STARTED,
    REQUEST_PREPARED,
    REQUEST_SENT,
    RESPONSE_RECEIVED,
    PARSING_RESPONSE,
    SAVING_RESULT,
    SUCCEEDED,
    FAILED,
    COMPLETE,
}

/**
 * The full technical trace for one email's processing attempt — populated only when
 * [EmailDebugConfig.ENABLED]. Every field is nullable/empty by default because it's filled in
 * incrementally as processing advances through [EmailProcessingPhase]; a UI showing this should
 * treat a null field as "not reached yet" rather than an error.
 */
data class EmailProcessingDebugInfo(
    val emailId: String,
    val accountLabel: String,
    val requestBodyJson: String = "",
    val httpStatusCode: Int? = null,
    val httpSuccess: Boolean? = null,
    val rawResponseBody: String? = null,
    val parsedResultJson: String? = null,
    val parseErrorMessage: String? = null,
    val failureStage: FailureStage = FailureStage.NONE,
    val failureReason: String? = null,
)

/**
 * One progress event for the currently running sequential batch — see
 * [InsightRepository.processAllPendingForAccount]. [currentIndex] is 1-based; 0 only for the
 * initial STARTED event. [succeededCount]/[failedCount] reflect everything completed so far,
 * *including* the item this event is reporting on for SUCCEEDED/FAILED phases. [debugInfo] is
 * only populated when [EmailDebugConfig.ENABLED].
 */
data class EmailProcessingProgress(
    val totalCount: Int,
    val currentIndex: Int,
    val currentEmailLabel: String,
    val phase: EmailProcessingPhase,
    val succeededCount: Int,
    val failedCount: Int,
    val debugInfo: EmailProcessingDebugInfo? = null,
)
