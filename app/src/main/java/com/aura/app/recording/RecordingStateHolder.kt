package com.aura.app.recording

import com.aura.app.domain.model.RecordingStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-wide source of truth for recording state, shared by the foreground
 * service, the QS tile, and the UI so they never disagree about whether
 * recording is active.
 */
@Singleton
class RecordingStateHolder @Inject constructor() {

    private val _status = MutableStateFlow<RecordingStatus>(RecordingStatus.Idle)
    val status: StateFlow<RecordingStatus> = _status

    fun setRecording(startedAtMillis: Long) {
        _status.value = RecordingStatus.Recording(startedAtMillis)
    }

    fun setIdle() {
        _status.value = RecordingStatus.Idle
    }
}
