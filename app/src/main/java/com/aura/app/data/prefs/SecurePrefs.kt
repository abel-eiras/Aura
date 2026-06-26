package com.aura.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aura.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted storage for the signed-in account email, cached Drive access token, and Drive folder id. */
@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            Constants.SECURE_PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var accountEmail: String?
        get() = prefs.getString(Constants.KEY_ACCOUNT_EMAIL, null)
        set(value) = prefs.edit().putString(Constants.KEY_ACCOUNT_EMAIL, value).apply()

    var driveFolderId: String?
        get() = prefs.getString(Constants.KEY_DRIVE_FOLDER_ID, null)
        set(value) = prefs.edit().putString(Constants.KEY_DRIVE_FOLDER_ID, value).apply()

    var cachedAccessToken: String?
        get() = prefs.getString(Constants.KEY_CACHED_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(Constants.KEY_CACHED_ACCESS_TOKEN, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
