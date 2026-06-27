package com.aura.app.domain.model

/** Recordings are uncompressed 16-bit PCM WAV, so quality is just the sample rate. */
enum class AudioQuality(val sampleRate: Int) {
    LOW(22_050),
    NORMAL(44_100),
    HIGH(48_000)
}
