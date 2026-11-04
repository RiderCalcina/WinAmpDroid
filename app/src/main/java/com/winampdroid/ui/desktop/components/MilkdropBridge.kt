package com.winampdroid.ui.desktop.components

import android.webkit.JavascriptInterface

/**
 * Thread-safe Bridge between Android Native Audio and Chromium WebGL Milkdrop.
 *
 * Eliminates high-frequency evaluateJavascript() IPC calls that saturate
 * the Android Main Looper and freeze UI controls after playing several songs.
 * WebGL shader pulls audio values directly on requestAnimationFrame.
 */
class MilkdropBridge {
    @Volatile private var bass: Float = 0f
    @Volatile private var mid: Float = 0f
    @Volatile private var treble: Float = 0f
    @Volatile private var isPlaying: Boolean = false
    @Volatile private var isPaused: Boolean = false

    @JavascriptInterface
    fun getBass(): Float = bass

    @JavascriptInterface
    fun getMid(): Float = mid

    @JavascriptInterface
    fun getTreble(): Float = treble

    @JavascriptInterface
    fun getPlaybackState(): Int = when {
        !isPlaying && !isPaused -> 0
        isPaused -> 2
        else -> 1
    }

    fun updateAudio(b: Float, m: Float, t: Float) {
        bass = b
        mid = m
        treble = t
    }

    fun updateState(playing: Boolean, paused: Boolean) {
        isPlaying = playing
        isPaused = paused
    }
}

