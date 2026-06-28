package com.aura.app.data.update

import com.aura.app.domain.model.UpdateInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class UpdateCheckRepository @Inject constructor(
    private val gitHubApi: GitHubApi
) {

    /** Null on network failure or when already on the latest version. */
    suspend fun checkForUpdate(currentVersionName: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val release = runCatching { gitHubApi.getLatestRelease() }.getOrNull() ?: return@withContext null
        val latestVersion = release.tagName.removePrefix("v")
        if (!isNewer(latestVersion, currentVersionName)) return@withContext null
        UpdateInfo(versionName = latestVersion, releaseUrl = release.htmlUrl)
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = candidate.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val length = maxOf(candidateParts.size, currentParts.size)
        for (i in 0 until length) {
            val c = candidateParts.getOrElse(i) { 0 }
            val cur = currentParts.getOrElse(i) { 0 }
            if (c != cur) return c > cur
        }
        return false
    }
}
