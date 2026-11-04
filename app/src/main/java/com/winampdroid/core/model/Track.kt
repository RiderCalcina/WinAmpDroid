package com.winampdroid.core.model

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Double = 0.0,
    val uri: Uri,
    val isAsset: Boolean = false
) {
    fun formatDuration(): String {
        if (durationSeconds <= 0.0) return "LIVE"
        val totalSecs = durationSeconds.toInt()
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

