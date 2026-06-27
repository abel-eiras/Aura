package com.aura.app.domain.model

/** Each language's [displayName] is its own native name, shown the same way regardless of the
 * currently active UI language (a language picker doesn't translate its own entries). */
enum class AppLanguage(val tag: String, val displayName: String) {
    SPANISH("es-ES", "Español"),
    GALICIAN("gl", "Galego"),
    ENGLISH("en", "English");

    companion object {
        val DEFAULT = SPANISH

        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: DEFAULT
    }
}
