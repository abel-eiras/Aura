package com.aura.app.data.drive.model

data class DriveFileMetadata(
    val name: String,
    val mimeType: String? = null,
    val parents: List<String>? = null
)

data class DriveFile(
    val id: String,
    val name: String? = null
)

data class DriveFileListResponse(
    val files: List<DriveFile> = emptyList()
)
