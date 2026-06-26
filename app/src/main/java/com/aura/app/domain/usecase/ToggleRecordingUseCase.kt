package com.aura.app.domain.usecase

import android.content.Context
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.recording.RecordingService
import com.aura.app.recording.RecordingStateHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ToggleRecordingUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingStateHolder: RecordingStateHolder
) {
    operator fun invoke() {
        when (recordingStateHolder.status.value) {
            is RecordingStatus.Idle -> RecordingService.start(context)
            is RecordingStatus.Recording, is RecordingStatus.Paused -> RecordingService.stop(context)
        }
    }
}

/** Pauses while recording, resumes while paused; no-op while idle. */
class TogglePauseRecordingUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingStateHolder: RecordingStateHolder
) {
    operator fun invoke() {
        when (recordingStateHolder.status.value) {
            is RecordingStatus.Recording -> RecordingService.pause(context)
            is RecordingStatus.Paused -> RecordingService.resume(context)
            is RecordingStatus.Idle -> Unit
        }
    }
}
