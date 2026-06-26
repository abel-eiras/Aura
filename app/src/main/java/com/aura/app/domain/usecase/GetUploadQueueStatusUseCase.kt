package com.aura.app.domain.usecase

import com.aura.app.data.upload.UploadQueueRepository
import com.aura.app.domain.model.UploadQueueStatus
import javax.inject.Inject

class GetUploadQueueStatusUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    operator fun invoke(): UploadQueueStatus = uploadQueueRepository.queueStatus()
}
