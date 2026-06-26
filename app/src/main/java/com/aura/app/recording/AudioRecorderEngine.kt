package com.aura.app.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.aura.app.util.FileNaming
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaRecorder wrapper. Opus-in-OGG (MediaRecorder.OutputFormat.OGG /
 * AudioEncoder.OPUS) only exists from API 29 onward, but minSdk is 26, so
 * API 26-28 falls back to AAC-in-MP4 (.m4a) at the same bitrate.
 */
@Singleton
class AudioRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    private val supportsOpusOgg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun start(outputDir: File): File {
        val extension = if (supportsOpusOgg) "ogg" else "m4a"
        val file = File(outputDir, FileNaming.newRecordingFileName(extension))

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            if (supportsOpusOgg) {
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            } else {
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(48_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        mediaRecorder = recorder
        outputFile = file
        return file
    }

    /** Returns the finished file, or null if the recording was too short/invalid to keep. */
    fun stop(): File? {
        val file = outputFile
        val recorder = mediaRecorder
        mediaRecorder = null
        outputFile = null

        return try {
            recorder?.stop()
            recorder?.release()
            file
        } catch (e: RuntimeException) {
            // stop() throws if called within ~1s of start() with no audio captured yet.
            recorder?.release()
            file?.delete()
            null
        }
    }
}
