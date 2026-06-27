package com.aura.app.domain.model

enum class AudioQuality(val bitRate: Int, val sampleRate: Int) {
    LOW(32_000, 44_100),
    NORMAL(64_000, 48_000),
    HIGH(128_000, 48_000)
}
