package com.inialpha.executiveai.data.repository

/** Where a single email is in its processing lifecycle, for UI progress display. */
enum class EmailProcessingPhase {
    /** Emitted once at the start with the full queue size and nothing processed yet. */
    STARTED,
    /** Currently being sent to / awaited from the AI gateway. */
    PROCESSING,
    SUCCEEDED,
    FAILED,
    /** Emitted once at the end, after every email in the queue has been handled. */
    COMPLETE,
}

/**
 * One progress event for the currently running sequential batch — see
 * [InsightRepository.processAllPendingForAccount]. [currentIndex] is 1-based; 0 only for the
 * initial STARTED event. [succeededCount]/[failedCount] reflect everything completed so far,
 * *including* the item this event is reporting on for SUCCEEDED/FAILED phases.
 */
data class EmailProcessingProgress(
    val totalCount: Int,
    val currentIndex: Int,
    val currentEmailLabel: String,
    val phase: EmailProcessingPhase,
    val succeededCount: Int,
    val failedCount: Int,
)
