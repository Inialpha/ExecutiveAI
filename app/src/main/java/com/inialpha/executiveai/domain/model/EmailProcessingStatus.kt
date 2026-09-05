package com.inialpha.executiveai.domain.model

/**
 * Per-email AI-processing status, persisted so a later synchronization can tell exactly which
 * emails still need work — see [com.inialpha.executiveai.data.repository.InsightRepository].
 *
 * PENDING: fetched from Gmail, not yet sent to the AI gateway.
 * COMPLETED: successfully processed; never reprocessed by a later sync.
 * FAILED: processing was attempted and did not succeed (network error, malformed/missing result,
 * app killed mid-request, etc.) — retried on the next synchronization, same as PENDING.
 */
enum class EmailProcessingStatus {
    PENDING,
    COMPLETED,
    FAILED,
}
