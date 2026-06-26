package com.aura.app.data.upload

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aura.app.data.drive.DriveRepository
import com.aura.app.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Uploads one recording to Drive. On failure it doesn't rely on WorkManager's
 * built-in backoff (which only multiplies by a fixed factor); instead it
 * re-enqueues itself with the exact 15s/30s/1m/5m/15m sequence and gives up
 * after MAX_UPLOAD_RETRIES attempts, leaving the file in place for the next
 * manual retry (app open / new recording) to pick up.
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val driveRepository: DriveRepository,
    private val uploadQueueRepository: UploadQueueRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ATTEMPT = "attempt"

        private val BACKOFF_DELAYS_MILLIS = longArrayOf(
            15_000L, 30_000L, 60_000L, 300_000L, 900_000L
        )
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val attempt = inputData.getInt(KEY_ATTEMPT, 0)
        val file = File(filePath)

        if (!file.exists()) return Result.success()

        val uploaded = driveRepository.uploadRecording(file)
        if (uploaded) {
            file.delete()
            return Result.success()
        }

        if (attempt >= Constants.MAX_UPLOAD_RETRIES) {
            return Result.failure()
        }

        val delay = BACKOFF_DELAYS_MILLIS[attempt.coerceIn(0, BACKOFF_DELAYS_MILLIS.lastIndex)]
        uploadQueueRepository.enqueueUpload(file, attempt = attempt + 1, delayMillis = delay)
        return Result.success()
    }
}
