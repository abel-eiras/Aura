package com.aura.app.domain.model

enum class UploadState { QUEUED, UPLOADED, FAILED }

/**
 * [filePath] is null for [UploadState.UPLOADED] entries: the audio file is deleted from the
 * device once Drive has it, so only the lightweight history record remains (see
 * [com.aura.app.data.upload.UploadHistoryStore]). Playback and retry aren't available for those.
 */
data class LocalRecording(
    val fileName: String,
    val filePath: String?,
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val uploadState: UploadState
)
