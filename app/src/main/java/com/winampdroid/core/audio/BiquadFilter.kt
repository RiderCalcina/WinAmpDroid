package com.winampdroid.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * High-precision Biquad IIR Filter for audio frequency bands.
 * Follows Robert Bristow-Johnson's Audio EQ Cookbook formulas.
 * Includes anti-denormal flush to prevent ARM CPU kernel traps.
 */
class BiquadFilter(
    val frequency: Float,
    private val q: Float = 1.4f
) {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // Filter state for left and right channels (Direct Form I)
    private var x1L = 0.0
    private var x2L = 0.0
    private var y1L = 0.0
    private var y2L = 0.0

    private var x1R = 0.0
    private var x2R = 0.0
    private var y1R = 0.0
    private var y2R = 0.0

    var currentGainDb: Float = 0f
        private set

    private var sampleRate: Int = 44100

    fun configure(sampleRate: Int, gainDb: Float) {
        this.sampleRate = sampleRate
        this.currentGainDb = gainDb

        if (gainDb == 0f) {
            b0 = 1.0; b1 = 0.0; b2 = 0.0; a1 = 0.0; a2 = 0.0
            reset()
            return
        }

        val w0 = 2.0 * PI * frequency / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val a = 10.0.pow(gainDb / 40.0)
        val cosW0 = cos(w0)

        val a0 = 1.0 + alpha / a
        b0 = (1.0 + alpha * a) / a0
        b1 = (-2.0 * cosW0) / a0
        b2 = (1.0 - alpha * a) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha / a) / a0
    }

    fun processSampleLeft(x: Double): Double {
        if (currentGainDb == 0f) return x
        var y = b0 * x + b1 * x1L + b2 * x2L - a1 * y1L - a2 * y2L
        // Anti-denormal flush (prevents ARM CPU floating-point kernel traps on silent/decaying signals)
        if (y > -1e-15 && y < 1e-15) y = 0.0
        x2L = x1L
        x1L = x
        y2L = y1L
        y1L = y
        return y
    }

    fun processSampleRight(x: Double): Double {
        if (currentGainDb == 0f) return x
        var y = b0 * x + b1 * x1R + b2 * x2R - a1 * y1R - a2 * y2R
        // Anti-denormal flush (prevents ARM CPU floating-point kernel traps on silent/decaying signals)
        if (y > -1e-15 && y < 1e-15) y = 0.0
        x2R = x1R
        x1R = x
        y2R = y1R
        y1R = y
        return y
    }

    fun reset() {
        x1L = 0.0; x2L = 0.0; y1L = 0.0; y2L = 0.0
        x1R = 0.0; x2R = 0.0; y1R = 0.0; y2R = 0.0
    }
}

