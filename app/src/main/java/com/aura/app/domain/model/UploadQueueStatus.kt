package com.aura.app.domain.model

data class UploadQueueStatus(
    val pendingCount: Int,
    val totalBytes: Long
)

data class AccountState(
    val email: String?
) {
    val isSignedIn: Boolean get() = email != null
}
