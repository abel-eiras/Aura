package com.aura.app.data.upload

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aura.app.domain.model.UploadQueueStatus
import com.aura.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadQueueRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {

    fun recordingsDir(): File =
        File(context.filesDir, Constants.RECORDINGS_DIR_NAME).apply { mkdirs() }

    /** Pending = files still sitting in app-private storage; they're deleted on successful upload. */
    fun queueStatus(): UploadQueueStatus {
        val files = recordingsDir().listFiles().orEmpty()
        return UploadQueueStatus(
            pendingCount = files.size,
            totalBytes = files.sumOf { it.length() }
        )
    }

    fun enqueueUpload(file: File, attempt: Int = 0, delayMillis: Long = 0L) {
        val workName = Constants.UPLOAD_WORK_NAME_PREFIX + file.name
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(
                Data.Builder()
                    .putString(UploadWorker.KEY_FILE_PATH, file.absolutePath)
                    .putInt(UploadWorker.KEY_ATTEMPT, attempt)
                    .build()
            )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    /** Called on app start / sign-in to retry anything left over from a previous session. */
    fun enqueueAllPending() {
        recordingsDir().listFiles().orEmpty().forEach { enqueueUpload(it) }
    }
}
