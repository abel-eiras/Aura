package com.aura.app.domain.model

sealed interface RecordingStatus {
    data object Idle : RecordingStatus
    data class Recording(val startedAtMillis: Long) : RecordingStatus
}
