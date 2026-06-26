package com.aura.app.domain.model

sealed interface RecordingStatus {
    data object Idle : RecordingStatus

    /** [accumulatedPausedMillis] is the total time already spent paused in earlier pause/resume cycles. */
    data class Recording(val startedAtMillis: Long, val accumulatedPausedMillis: Long = 0L) : RecordingStatus

    data class Paused(
        val startedAtMillis: Long,
        val pausedAtMillis: Long,
        val accumulatedPausedMillis: Long
    ) : RecordingStatus
}
