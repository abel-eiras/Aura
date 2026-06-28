package com.aura.app.data.upload

import android.content.Context
import androidx.core.content.edit
import com.aura.app.util.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class UploadHistoryEntry(
    val fileName: String,
    val sizeBytes: Long,
    val uploadedAtMillis: Long
)

/**
 * Lightweight record of recordings that finished uploading. The audio itself is deleted from
 * the device once Drive has it (see [UploadWorker]); this just remembers that it happened, so
 * the Recordings screen can show a real history instead of the file silently vanishing.
 */
@Singleton
class UploadHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    private val entryListType = object : TypeToken<List<UploadHistoryEntry>>() {}.type

    fun all(): List<UploadHistoryEntry> {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<UploadHistoryEntry>>(json, entryListType) }
            .getOrNull()
            .orEmpty()
    }

    fun add(entry: UploadHistoryEntry) {
        val updated = (listOf(entry) + all()).take(Constants.MAX_UPLOAD_HISTORY_ENTRIES)
        prefs.edit { putString(KEY_ENTRIES, gson.toJson(updated)) }
    }

    fun remove(fileName: String) {
        val updated = all().filterNot { it.fileName == fileName }
        prefs.edit { putString(KEY_ENTRIES, gson.toJson(updated)) }
    }

    private companion object {
        const val PREFS_FILE_NAME = "aura_upload_history"
        const val KEY_ENTRIES = "entries"
    }
}
