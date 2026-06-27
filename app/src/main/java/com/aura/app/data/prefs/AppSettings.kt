package com.aura.app.data.prefs

import android.content.Context
import androidx.core.content.edit
import com.aura.app.domain.model.AudioQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Plain user-facing preferences (not secret, unlike SecurePrefs' encrypted auth storage). */
@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    var wifiOnlyUpload: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY_UPLOAD, false)
        set(value) = prefs.edit { putBoolean(KEY_WIFI_ONLY_UPLOAD, value) }

    var audioQuality: AudioQuality
        get() = AudioQuality.entries.find { it.name == prefs.getString(KEY_AUDIO_QUALITY, null) }
            ?: AudioQuality.NORMAL
        set(value) = prefs.edit { putString(KEY_AUDIO_QUALITY, value.name) }

    private companion object {
        const val PREFS_FILE_NAME = "aura_app_settings"
        const val KEY_WIFI_ONLY_UPLOAD = "wifi_only_upload"
        const val KEY_AUDIO_QUALITY = "audio_quality"
    }
}
