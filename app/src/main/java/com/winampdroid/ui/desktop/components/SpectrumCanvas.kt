package com.winampdroid.ui.desktop.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.winampdroid.core.skin.SkinColors
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class VisualizerMode {
    BARS,
    OSCILLOSCOPE,
    OFF
}

/**
 * Classic Winamp 75x16 pixel visualizer canvas.
 * Implements spectrum analyzer bars with peak gravity decay and oscilloscope line.
 * Fills the exact LCD area under the digital LED time display (75 x 16 virtual pixels).
 */
@Composable
fun SpectrumCanvas(
    mode: VisualizerMode,
    spectralData: FloatArray,
    pcmWaveData: ShortArray,
    skinColors: SkinColors,
    modifier: Modifier = Modifier
) {
    val barCount = 19
    val barWidth = 3f
    val barGap = 1f
    val visHeight = 16f

    // Peak tracking and gravity falloff arrays
    val peakHeights = remember { FloatArray(barCount) { 0f } }
    val barHeights = remember { FloatArray(barCount) { 0f } }
    val peakVelocities = remember { FloatArray(barCount) { 0.25f } }

    // Authentic Winamp hybrid log-linear frequency mapping for 19 bars (across 512 FFT bins)
    val binRanges = remember(spectralData.size) {
        val size = spectralData.size.coerceAtLeast(1)
        val maxFreq = min(512, size)
        val scale = 0.91 // Winamp 0.91 log factor
        val logMax = log10(maxFreq.toDouble().coerceAtLeast(2.0))
        IntArray(barCount * 2).apply {
            for (i in 0 until barCount) {
                val fracStart = (i * 4).toDouble() / 75.0
                val fracEnd = min(1.0, ((i + 1) * 4).toDouble() / 75.0)

                val linStart = fracStart * (maxFreq - 1)
                val logStart = 10.0.pow(fracStart * logMax)
                val idxStart = ((1.0 - scale) * linStart + scale * logStart).toInt().coerceIn(0, maxFreq - 1)

                val linEnd = fracEnd * (maxFreq - 1)
                val logEnd = 10.0.pow(fracEnd * logMax)
                val idxEnd = ((1.0 - scale) * linEnd + scale * logEnd).toInt().coerceIn(idxStart + 1, maxFreq)

                this[i * 2] = idxStart
                this[i * 2 + 1] = idxEnd
            }
        }
    }

    val colors = skinColors.visColors

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(colors[0]))
    ) {
        if (mode == VisualizerMode.OFF) return@Canvas

        val wScale = size.width / 75f
        val hScale = size.height / 16f

        if (mode == VisualizerMode.BARS) {
            for (i in 0 until barCount) {
                val binStart = binRanges[i * 2]
                val binEnd = binRanges[i * 2 + 1]

                var maxVal = 0f
                var sumVal = 0f
                val count = max(1, binEnd - binStart)
                for (b in binStart until binEnd) {
                    if (b < spectralData.size) {
                        val v = spectralData[b]
                        if (v > maxVal) maxVal = v
                        sumVal += v
                    }
                }
                // Mix 75% peak transient + 25% average body for punchy yet solid bars
                val bandVal = (maxVal * 0.75f) + ((sumVal / count) * 0.25f)

                // Highs equalization boost
                val highBoost = 1.0f + (i.toFloat() / barCount) * 0.45f
                val targetHeight = (bandVal * 2.7f * highBoost).coerceIn(0f, 16f)

                // Instant rise on audio beat/transient
                if (targetHeight >= barHeights[i]) {
                    barHeights[i] = targetHeight
                } else {
                    // Brisk Winamp decay: ~1.25 blocks per frame at 60 FPS (~210ms full drop)
                    barHeights[i] = max(0f, barHeights[i] - 1.25f)
                }

                // Authentic Winamp peak gravity acceleration
                if (barHeights[i] >= peakHeights[i]) {
                    peakHeights[i] = barHeights[i]
                    peakVelocities[i] = 0.25f // initial hang
                } else {
                    peakHeights[i] = max(0f, peakHeights[i] - peakVelocities[i])
                    peakVelocities[i] = min(1.6f, peakVelocities[i] * 1.15f) // accelerating gravity
                }

                val x = i * (barWidth + barGap) * wScale
                val bh = barHeights[i]

                // Draw vertical bar bands using viscolor palette (colors 2 to 17)
                for (y in 0 until bh.toInt()) {
                    val colorIdx = (17 - y).coerceIn(2, 17)
                    drawRect(
                        color = Color(colors[colorIdx]),
                        topLeft = Offset(x, (visHeight - y - 1) * hScale),
                        size = Size(barWidth * wScale, 1f * hScale)
                    )
                }

                // Draw peak dot (color 18) strictly floating above the active bar
                val peakY = peakHeights[i].toInt()
                if (peakY in 1..15 && peakY > bh.toInt()) {
                    drawRect(
                        color = Color(colors[18]),
                        topLeft = Offset(x, (visHeight - peakY - 1) * hScale),
                        size = Size(barWidth * wScale, 1f * hScale)
                    )
                }
            }
        } else if (mode == VisualizerMode.OSCILLOSCOPE) {
            // Oscilloscope Waveform Mode across full 75px width
            val step = max(1, pcmWaveData.size / 75)
            val oscColor = Color(colors[19])

            for (x in 0 until 74) {
                val sIdx1 = (x * step).coerceIn(pcmWaveData.indices)
                val sIdx2 = ((x + 1) * step).coerceIn(pcmWaveData.indices)

                val y1 = 8f + (pcmWaveData[sIdx1] / 32768f) * 7f
                val y2 = 8f + (pcmWaveData[sIdx2] / 32768f) * 7f

                drawLine(
                    color = oscColor,
                    start = Offset(x * wScale, y1 * hScale),
                    end = Offset((x + 1) * wScale, y2 * hScale),
                    strokeWidth = 1.2f * hScale
                )
            }
        }
    }
}
