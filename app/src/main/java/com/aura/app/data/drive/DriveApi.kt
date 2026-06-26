package com.aura.app.data.drive

import com.aura.app.data.drive.model.DriveFile
import com.aura.app.data.drive.model.DriveFileListResponse
import com.aura.app.data.drive.model.DriveFileMetadata
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

/** Thin wrapper over the Drive REST v3 endpoints we actually use - no Drive Android SDK. */
interface DriveApi {

    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String,
        @Query("spaces") spaces: String,
        @Query("fields") fields: String
    ): DriveFileListResponse

    @POST("drive/v3/files")
    suspend fun createFile(
        @Header("Authorization") authHeader: String,
        @Query("fields") fields: String,
        @Body metadata: DriveFileMetadata
    ): DriveFile

    @POST
    suspend fun uploadMultipart(
        @Header("Authorization") authHeader: String,
        @Url url: String,
        @Body body: RequestBody
    ): DriveFile
}
