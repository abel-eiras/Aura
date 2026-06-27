package com.aura.app.data.update

import com.aura.app.util.Constants
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = Constants.GITHUB_OWNER,
        @Path("repo") repo: String = Constants.GITHUB_REPO
    ): GitHubRelease
}
