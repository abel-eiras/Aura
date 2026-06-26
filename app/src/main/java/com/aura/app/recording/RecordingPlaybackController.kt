package com.aura.app.recording

import android.media.MediaPlayer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Plays at most one local recording at a time; used by the Recordings screen. */
@Singleton
class RecordingPlaybackController @Inject constructor() {

    private var mediaPlayer: MediaPlayer? = null

    private val _playingFilePath = MutableStateFlow<String?>(null)
    val playingFilePath: StateFlow<String?> = _playingFilePath

    /** Starts playback of [filePath], or stops it if it's already the one playing. */
    fun togglePlayback(filePath: String) {
        if (_playingFilePath.value == filePath) {
            stop()
            return
        }
        stop()
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            _playingFilePath.value = filePath
        }
    }

    fun stop() {
        mediaPlayer?.let { runCatching { it.release() } }
        mediaPlayer = null
        _playingFilePath.value = null
    }
}
