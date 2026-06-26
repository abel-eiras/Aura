package com.aura.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileNaming {

    private fun timestampFormatter() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /** e.g. aura_20250626_143022.ogg (or .m4a on API < 29, see AudioRecorderEngine). */
    fun newRecordingFileName(extension: String): String {
        val timestamp = timestampFormatter().format(Date())
        return "aura_$timestamp.$extension"
    }
}
