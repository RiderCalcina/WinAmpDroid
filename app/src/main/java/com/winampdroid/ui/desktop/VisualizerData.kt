package com.winampdroid.ui.desktop

import androidx.compose.runtime.Stable

/**
 * High-performance container for visualizer spectrum and wave data.
 * Marked @Stable to allow Compose to skip recomposition when passing data providers.
 */
@Stable
class VisualizerData(
    val spectralData: FloatArray = FloatArray(512) { 0f },
    val pcmWaveData: ShortArray = ShortArray(0)
)

