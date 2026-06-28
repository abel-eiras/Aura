package com.aura.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.auth.GoogleAuthManager
import com.aura.app.data.prefs.AppSettings
import com.aura.app.domain.model.AccountState
import com.aura.app.domain.model.AppLanguage
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    private val _signInError = MutableStateFlow<String?>(null)
    val signInError: StateFlow<String?> = _signInError

    private val _language = MutableStateFlow(AppLanguage.fromTag(appSettings.languageTag))
    val language: StateFlow<AppLanguage> = _language

    init {
        authManager.needsReauth.onEach { needsReauth ->
            if (_accountState.value.needsReauth != needsReauth) {
                _accountState.value = _accountState.value.copy(needsReauth = needsReauth)
            }
        }.launchIn(viewModelScope)
    }

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

    fun setLanguage(language: AppLanguage) {
        appSettings.languageTag = language.tag
        _language.value = language
    }

    fun signInIntent(): Intent = authManager.signInIntent()

    fun onSignInResult(data: Intent?) {
        val account = authManager.handleSignInResult(data)
        if (account != null) {
            _signInError.value = null
            refresh()
            retryPendingUploadsUseCase()
        } else {
            _signInError.value = authManager.lastSignInErrorMessage
        }
    }

    fun dismissSignInError() {
        _signInError.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            refresh()
        }
    }

    /** Intent to re-grant Drive consent without a full sign-out, or null if none is pending. */
    fun reauthIntent(): Intent? = authManager.consumeRecoveryIntent()

    fun onReauthResult() {
        refresh()
        retryPendingUploadsUseCase()
    }

    fun refresh() {
        _accountState.value = getAccountStateUseCase()
        _queueStatus.value = getUploadQueueStatusUseCase()
    }
}
