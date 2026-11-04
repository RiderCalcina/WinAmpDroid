package com.winampdroid.core.model

import androidx.compose.ui.unit.IntOffset

enum class WindowType {
    MAIN,
    EQUALIZER,
    PLAYLIST
}

data class WindowState(
    val type: WindowType,
    val x: Float = 0f,
    val y: Float = 0f,
    val isOpen: Boolean = true,
    val isShade: Boolean = false,
    val width: Int = 275,
    val height: Int = 116,
    val shadeHeight: Int = 14,
    val zIndex: Float = 0f
) {
    fun currentHeight(): Int = if (isShade) shadeHeight else height

    companion object {
        const val SNAP_DISTANCE = 10f // Snap threshold in virtual desktop units
    }
}

