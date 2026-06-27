package com.aura.app.util

object Constants {
    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    const val DRIVE_FOLDER_NAME = "Aura"
    const val DRIVE_FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
    const val DRIVE_UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"
    const val DRIVE_MIME_FOLDER = "application/vnd.google-apps.folder"

    const val SECURE_PREFS_FILE_NAME = "aura_secure_prefs"
    const val KEY_ACCOUNT_EMAIL = "account_email"
    const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
    const val KEY_CACHED_ACCESS_TOKEN = "cached_access_token"

    const val RECORDINGS_DIR_NAME = "recordings"
    const val UPLOAD_WORK_NAME_PREFIX = "aura_upload_"
    const val NOTIFICATION_CHANNEL_ID = "aura_recording_channel"
    const val NOTIFICATION_ID = 1001

    const val MAX_UPLOAD_RETRIES = 5

    /** Above this, oldest pending recordings are deleted so local storage can't grow unbounded. */
    const val MAX_PENDING_STORAGE_BYTES = 500L * 1024 * 1024

    /** Caps the persisted "uploaded" history so it can't grow unbounded either. */
    const val MAX_UPLOAD_HISTORY_ENTRIES = 200
}
