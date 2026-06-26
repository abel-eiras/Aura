package com.aura.app.data.drive

import com.aura.app.data.auth.GoogleAuthManager
import com.aura.app.data.drive.model.DriveFileMetadata
import com.aura.app.data.prefs.SecurePrefs
import com.aura.app.util.Constants
import com.google.gson.Gson
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class DriveRepository @Inject constructor(
    private val driveApi: DriveApi,
    private val authManager: GoogleAuthManager,
    private val securePrefs: SecurePrefs,
    private val gson: Gson
) {

    /** Uploads [file] into the app's "Aura" Drive folder. Returns true on success. */
    suspend fun uploadRecording(file: File): Boolean = withContext(Dispatchers.IO) {
        val token = currentAccessToken() ?: return@withContext false
        val folderId = ensureAuraFolderId(token) ?: return@withContext false

        val metadata = DriveFileMetadata(
            name = file.name,
            mimeType = mimeTypeFor(file.extension),
            parents = listOf(folderId)
        )
        val metadataBody = gson.toJson(metadata).toRequestBody("application/json; charset=UTF-8".toMediaType())
        val mediaBody = file.asRequestBody(mimeTypeFor(file.extension).toMediaType())

        val multipartBody: RequestBody = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(MultipartBody.Part.create(metadataBody))
            .addPart(MultipartBody.Part.create(mediaBody))
            .build()

        runCatching {
            driveApi.uploadMultipart(
                authHeader = "Bearer $token",
                url = "${Constants.DRIVE_UPLOAD_ENDPOINT}?uploadType=multipart&fields=id,name",
                body = multipartBody
            )
        }.isSuccess
    }

    private suspend fun ensureAuraFolderId(token: String): String? {
        securePrefs.driveFolderId?.let { return it }

        val query = "mimeType='${Constants.DRIVE_MIME_FOLDER}' and name='${Constants.DRIVE_FOLDER_NAME}' " +
            "and trashed=false"
        val existing = runCatching {
            driveApi.listFiles(
                authHeader = "Bearer $token",
                query = query,
                spaces = "drive",
                fields = "files(id,name)"
            )
        }.getOrNull()?.files?.firstOrNull()

        val folderId = existing?.id ?: runCatching {
            driveApi.createFile(
                authHeader = "Bearer $token",
                fields = "id,name",
                metadata = DriveFileMetadata(
                    name = Constants.DRIVE_FOLDER_NAME,
                    mimeType = Constants.DRIVE_MIME_FOLDER
                )
            )
        }.getOrNull()?.id

        folderId?.let { securePrefs.driveFolderId = it }
        return folderId
    }

    private suspend fun currentAccessToken(): String? {
        return authManager.fetchFreshAccessToken() ?: securePrefs.cachedAccessToken
    }

    private fun mimeTypeFor(extension: String): String = when (extension.lowercase()) {
        "ogg" -> "audio/ogg"
        "m4a" -> "audio/mp4"
        else -> "application/octet-stream"
    }
}
