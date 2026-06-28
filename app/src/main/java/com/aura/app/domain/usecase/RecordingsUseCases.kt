package com.aura.app.domain.usecase

import com.aura.app.data.upload.UploadQueueRepository
import com.aura.app.domain.model.LocalRecording
import java.io.File
import javax.inject.Inject

class GetRecordingsUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    suspend operator fun invoke(): List<LocalRecording> = uploadQueueRepository.listRecordings()
}

/** Forces an immediate retry of one recording, bypassing whatever backoff it was on. */
class RetryUploadUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    operator fun invoke(filePath: String) {
        uploadQueueRepository.enqueueUpload(File(filePath))
    }
}

class DeleteRecordingUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    operator fun invoke(filePath: String) = uploadQueueRepository.deleteRecording(filePath)
}

/** Removes an already-uploaded recording from the local history list (Drive is untouched). */
class RemoveUploadHistoryEntryUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    operator fun invoke(fileName: String) = uploadQueueRepository.removeHistoryEntry(fileName)
}
