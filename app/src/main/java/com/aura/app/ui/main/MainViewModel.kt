package com.aura.app.ui.main

import androidx.lifecycle.ViewModel
import com.aura.app.domain.model.AccountState
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.domain.usecase.GetAccountStateUseCase
import com.aura.app.domain.usecase.RetryPendingUploadsUseCase
import com.aura.app.domain.usecase.ToggleRecordingUseCase
import com.aura.app.domain.usecase.TogglePauseRecordingUseCase
import com.aura.app.recording.RecordingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val toggleRecordingUseCase: ToggleRecordingUseCase,
    private val togglePauseRecordingUseCase: TogglePauseRecordingUseCase,
    private val getAccountStateUseCase: GetAccountStateUseCase,
    retryPendingUploadsUseCase: RetryPendingUploadsUseCase,
    recordingStateHolder: RecordingStateHolder
) : ViewModel() {

    val recordingStatus: StateFlow<RecordingStatus> = recordingStateHolder.status

    private val _accountState = MutableStateFlow(getAccountStateUseCase())
    val accountState: StateFlow<AccountState> = _accountState

    init {
        // Anything left over from a previous session (e.g. a permanently failed
        // upload, or one queued while offline) gets another chance on a normal app open.
        retryPendingUploadsUseCase()
    }

    fun refreshAccountState() {
        _accountState.value = getAccountStateUseCase()
    }

    fun toggleRecording() {
        toggleRecordingUseCase()
    }

    fun togglePause() {
        togglePauseRecordingUseCase()
    }
}
