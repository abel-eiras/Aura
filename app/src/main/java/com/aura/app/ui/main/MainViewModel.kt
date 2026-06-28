package com.aura.app.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.prefs.AppSettings
import com.aura.app.domain.model.AccountState
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.domain.model.UpdateInfo
import com.aura.app.domain.usecase.CheckForUpdateUseCase
import com.aura.app.domain.usecase.GetAccountStateUseCase
import com.aura.app.domain.usecase.RetryPendingUploadsUseCase
import com.aura.app.domain.usecase.ToggleRecordingUseCase
import com.aura.app.domain.usecase.TogglePauseRecordingUseCase
import com.aura.app.recording.RecordingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toggleRecordingUseCase: ToggleRecordingUseCase,
    private val togglePauseRecordingUseCase: TogglePauseRecordingUseCase,
    private val getAccountStateUseCase: GetAccountStateUseCase,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val appSettings: AppSettings,
    retryPendingUploadsUseCase: RetryPendingUploadsUseCase,
    recordingStateHolder: RecordingStateHolder
) : ViewModel() {

    val recordingStatus: StateFlow<RecordingStatus> = recordingStateHolder.status

    private val _accountState = MutableStateFlow(getAccountStateUseCase())
    val accountState: StateFlow<AccountState> = _accountState

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    init {
        // Anything left over from a previous session (e.g. a permanently failed
        // upload, or one queued while offline) gets another chance on a normal app open.
        retryPendingUploadsUseCase()
        checkForUpdate()
    }

    private fun checkForUpdate() {
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: return
        viewModelScope.launch {
            val info = checkForUpdateUseCase(versionName)
            if (info != null && info.versionName != appSettings.dismissedUpdateVersion) {
                _updateInfo.value = info
            }
        }
    }

    fun dismissUpdate() {
        appSettings.dismissedUpdateVersion = _updateInfo.value?.versionName
        _updateInfo.value = null
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
