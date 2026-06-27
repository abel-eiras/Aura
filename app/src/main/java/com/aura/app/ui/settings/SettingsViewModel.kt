package com.aura.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.auth.GoogleAuthManager
import com.aura.app.data.prefs.AppSettings
import com.aura.app.domain.model.AccountState
import com.aura.app.domain.model.AudioQuality
import com.aura.app.domain.model.UploadQueueStatus
import com.aura.app.domain.usecase.GetAccountStateUseCase
import com.aura.app.domain.usecase.GetUploadQueueStatusUseCase
import com.aura.app.domain.usecase.RetryPendingUploadsUseCase
import com.aura.app.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: GoogleAuthManager,
    private val signOutUseCase: SignOutUseCase,
    private val getAccountStateUseCase: GetAccountStateUseCase,
    private val getUploadQueueStatusUseCase: GetUploadQueueStatusUseCase,
    private val retryPendingUploadsUseCase: RetryPendingUploadsUseCase,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _accountState = MutableStateFlow(getAccountStateUseCase())
    val accountState: StateFlow<AccountState> = _accountState

    private val _queueStatus = MutableStateFlow(getUploadQueueStatusUseCase())
    val queueStatus: StateFlow<UploadQueueStatus> = _queueStatus

    private val _wifiOnlyUpload = MutableStateFlow(appSettings.wifiOnlyUpload)
    val wifiOnlyUpload: StateFlow<Boolean> = _wifiOnlyUpload

    private val _audioQuality = MutableStateFlow(appSettings.audioQuality)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality

    val versionName: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "1.0.0"

    fun setWifiOnlyUpload(enabled: Boolean) {
        appSettings.wifiOnlyUpload = enabled
        _wifiOnlyUpload.value = enabled
    }

    fun setAudioQuality(quality: AudioQuality) {
        appSettings.audioQuality = quality
        _audioQuality.value = quality
    }

    fun signInIntent(): Intent = authManager.signInIntent()

    fun onSignInResult(data: Intent?) {
        val account = authManager.handleSignInResult(data)
        if (account != null) {
            refresh()
            retryPendingUploadsUseCase()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            refresh()
        }
    }

    fun refresh() {
        _accountState.value = getAccountStateUseCase()
        _queueStatus.value = getUploadQueueStatusUseCase()
    }
}
