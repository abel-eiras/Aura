package com.aura.app.domain.model

enum class UploadState { QUEUED, FAILED }

data class LocalRecording(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val uploadState: UploadState
)
