package com.aura.app.util

import android.content.Context
import android.content.res.Configuration
import com.aura.app.data.prefs.AppSettings
import java.util.Locale

/**
 * Wraps a Context with the user's chosen app language, independent of the device's system
 * language. Applied via attachBaseContext() in every Context that calls getString()/stringResource
 * (Application, MainActivity, RecordingService, AuraTileService) so the override is consistent
 * everywhere, not just in Compose screens.
 */
object LocaleHelper {
    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(AppSettings.currentLanguageTag(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
