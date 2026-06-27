package com.aura.app.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.domain.model.LocalRecording
import com.aura.app.domain.usecase.DeleteRecordingUseCase
import com.aura.app.domain.usecase.GetRecordingsUseCase
import com.aura.app.domain.usecase.RemoveUploadHistoryEntryUseCase
import com.aura.app.domain.usecase.RetryUploadUseCase
import com.aura.app.recording.RecordingPlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val retryUploadUseCase: RetryUploadUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val removeUploadHistoryEntryUseCase: RemoveUploadHistoryEntryUseCase,
    private val playbackController: RecordingPlaybackController
) : ViewModel() {

    private val _recordings = MutableStateFlow<List<LocalRecording>>(emptyList())
    val recordings: StateFlow<List<LocalRecording>> = _recordings

    val playingFilePath: StateFlow<String?> = playbackController.playingFilePath

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _recordings.value = getRecordingsUseCase()
        }
    }

    fun retry(filePath: String) {
        retryUploadUseCase(filePath)
        refresh()
    }

    fun delete(recording: LocalRecording) {
        val filePath = recording.filePath
        if (filePath != null) {
            if (playingFilePath.value == filePath) playbackController.stop()
            deleteRecordingUseCase(filePath)
        } else {
            removeUploadHistoryEntryUseCase(recording.fileName)
        }
        refresh()
    }

    fun togglePlayback(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playbackController.togglePlayback(filePath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackController.stop()
    }
}
