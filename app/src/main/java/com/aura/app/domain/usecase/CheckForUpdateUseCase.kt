package com.aura.app.domain.usecase

import com.aura.app.data.update.UpdateCheckRepository
import com.aura.app.domain.model.UpdateInfo
import javax.inject.Inject

class CheckForUpdateUseCase @Inject constructor(
    private val updateCheckRepository: UpdateCheckRepository
) {
    suspend operator fun invoke(currentVersionName: String): UpdateInfo? =
        updateCheckRepository.checkForUpdate(currentVersionName)
}
