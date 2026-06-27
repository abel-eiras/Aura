package com.aura.app.recording

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.aura.app.data.prefs.AppSettings
import com.aura.app.util.FileNaming
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

/**
 * Records uncompressed 16-bit PCM straight to a WAV file via [AudioRecord], instead of relying
 * on MediaRecorder's encoders (which can't produce WAV/MP3 - only lossy formats like AAC/Opus).
 * A capture thread streams PCM bytes to disk as they arrive; the 44-byte WAV header is written
 * as a placeholder on start and patched with the real sizes once the final byte count is known.
 */
@Singleton
class AudioRecorderEngine @Inject constructor(
    private val appSettings: AppSettings
) {
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var outputFile: File? = null
    private var sampleRateUsed: Int = 0

    @Volatile private var isCapturing = false
    @Volatile private var isPaused = false
    @Volatile private var bytesWritten = 0L

    fun start(outputDir: File): File {
        val sampleRate = appSettings.audioQuality.sampleRate
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = (if (minBufferSize > 0) minBufferSize else sampleRate * 2) * 2

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        val file = File(outputDir, FileNaming.newRecordingFileName("wav"))
        RandomAccessFile(file, "rw").use { it.setLength(WAV_HEADER_SIZE.toLong()) }

        sampleRateUsed = sampleRate
        bytesWritten = 0L
        isPaused = false
        isCapturing = true
        outputFile = file
        audioRecord = record

        record.startRecording()
        captureThread = thread(name = "AuraAudioCapture") { captureLoop(record, file, bufferSize) }

        return file
    }

    private fun captureLoop(record: AudioRecord, file: File, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(WAV_HEADER_SIZE.toLong())
            while (isCapturing) {
                if (isPaused) {
                    Thread.sleep(50)
                    continue
                }
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    raf.write(buffer, 0, read)
                    bytesWritten += read
                }
            }
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    /** Returns the finished file, or null if the recording was too short/invalid to keep. */
    fun stop(): File? {
        isCapturing = false
        captureThread?.join(2000)
        captureThread = null

        runCatching { audioRecord?.stop() }
        audioRecord?.release()
        audioRecord = null

        val file = outputFile
        outputFile = null
        val totalBytes = bytesWritten

        if (file == null || totalBytes <= 0) {
            file?.delete()
            return null
        }

        writeWavHeader(file, totalBytes, sampleRateUsed)
        return file
    }

    private fun writeWavHeader(file: File, pcmDataSize: Long, sampleRate: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteArray(WAV_HEADER_SIZE)

        fun writeString(offset: Int, value: String) =
            value.forEachIndexed { i, c -> header[offset + i] = c.code.toByte() }

        fun writeIntLe(offset: Int, value: Int) {
            header[offset] = (value and 0xff).toByte()
            header[offset + 1] = ((value shr 8) and 0xff).toByte()
            header[offset + 2] = ((value shr 16) and 0xff).toByte()
            header[offset + 3] = ((value shr 24) and 0xff).toByte()
        }

        fun writeShortLe(offset: Int, value: Int) {
            header[offset] = (value and 0xff).toByte()
            header[offset + 1] = ((value shr 8) and 0xff).toByte()
        }

        writeString(0, "RIFF")
        writeIntLe(4, (pcmDataSize + 36).toInt())
        writeString(8, "WAVE")
        writeString(12, "fmt ")
        writeIntLe(16, 16)
        writeShortLe(20, 1)
        writeShortLe(22, channels)
        writeIntLe(24, sampleRate)
        writeIntLe(28, byteRate)
        writeShortLe(32, blockAlign)
        writeShortLe(34, bitsPerSample)
        writeString(36, "data")
        writeIntLe(40, pcmDataSize.toInt())

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }

    private companion object {
        const val WAV_HEADER_SIZE = 44
    }
}
