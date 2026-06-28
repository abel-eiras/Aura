package com.aura.app.domain.usecase

import com.aura.app.data.auth.GoogleAuthManager
import com.aura.app.data.prefs.SecurePrefs
import com.aura.app.data.upload.UploadQueueRepository
import com.aura.app.domain.model.AccountState
import javax.inject.Inject

class GetAccountStateUseCase @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val authManager: GoogleAuthManager
) {
    operator fun invoke(): AccountState = AccountState(
        email = securePrefs.accountEmail,
        needsReauth = authManager.needsReauth.value
    )
}

class SignOutUseCase @Inject constructor(
    private val authManager: GoogleAuthManager
) {
    suspend operator fun invoke() = authManager.signOut()
}

/** Called after a successful sign-in to kick off any recordings left over from before. */
class RetryPendingUploadsUseCase @Inject constructor(
    private val uploadQueueRepository: UploadQueueRepository
) {
    operator fun invoke() = uploadQueueRepository.enqueueAllPending()
}
