package com.aura.app.domain.model

data class UploadQueueStatus(
    val pendingCount: Int,
    val totalBytes: Long
)

data class AccountState(
    val email: String?,
    /** True when Drive access needs a fresh consent (token revoked/expired) - signed in, but
     * uploads will keep failing until the user reconnects. */
    val needsReauth: Boolean = false
) {
    val isSignedIn: Boolean get() = email != null
}
