package com.winampdroid.core.visualizer

import kotlin.math.*

/**
 * Port of Nullsoft Winamp / WACUP Fast Fourier Transform.
 * Translates time-domain audio samples into equalized frequency bands for the 75x16 px spectrum analyzer.
 */
class NullsoftFFT(
    private val samplesIn: Int = 1024,
    private val samplesOut: Int = 512
) {
    private val nfreq = samplesOut * 2
    private val bitRevTable = IntArray(nfreq)
    private val cosSinTable = mutableListOf<FloatArray>()
    private val envelope = FloatArray(samplesIn)
    private val equalize = FloatArray(nfreq / 2)

    private val temp1 = FloatArray(nfreq)
    private val temp2 = FloatArray(nfreq)

    companion object {
        private const val TWO_PI = 6.2831853f
        private const val HALF_PI = 1.5707963f
    }

    init {
        // Bit reversal table
        for (i in 0 until nfreq) bitRevTable[i] = i
        var j = 0
        for (i in 0 until nfreq) {
            if (j > i) {
                val temp = bitRevTable[i]
                bitRevTable[i] = bitRevTable[j]
                bitRevTable[j] = temp
            }
            var m = nfreq shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Cos/Sin table
        var dftSize = 2
        while (dftSize <= nfreq) {
            val theta = (-2.0 * Math.PI) / dftSize
            cosSinTable.add(floatArrayOf(cos(theta).toFloat(), sin(theta).toFloat()))
            dftSize = dftSize shl 1
        }

        // Sine envelope
        val mult = (1.0f / samplesIn) * TWO_PI
        for (i in 0 until samplesIn) {
            envelope[i] = (0.5f + 0.5f * sin(i * mult - HALF_PI))
        }

        // Logarithmic equalization table
        var bias = 0.04f
        val halfNfreq = nfreq / 2
        for (i in 0 until halfNfreq) {
            val invHalfNfreq = (9.0f - bias) / halfNfreq
            equalize[i] = log10(1.0f + bias + (i + 1) * invHalfNfreq)
            bias /= 1.0025f
        }
    }

    fun timeToFrequencyDomain(inWaveData: FloatArray, outSpectralData: FloatArray) {
        // 1. Setup input with envelope
        for (i in temp1.indices) {
            val idx = bitRevTable[i]
            temp1[i] = if (idx < inWaveData.size) inWaveData[idx] * envelope[idx] else 0f
        }
        temp2.fill(0f)

        // 2. Perform FFT
        val real = temp1
        val imag = temp2
        var dftSize = 2
        var t = 0

        while (dftSize <= temp1.size) {
            val wpr = cosSinTable[t][0]
            val wpi = cosSinTable[t][1]
            var wr = 1.0f
            var wi = 0.0f
            val hDftSize = dftSize shr 1

            for (m in 0 until hDftSize) {
                var i = m
                while (i < temp1.size) {
                    val k = i + hDftSize
                    val tempr = wr * real[k] - wi * imag[k]
                    val tempi = wr * imag[k] + wi * real[k]
                    real[k] = real[i] - tempr
                    imag[k] = imag[i] - tempi
                    real[i] += tempr
                    imag[i] += tempi
                    i += dftSize
                }
                val wTemp = wr
                wr = wr * wpr - wi * wpi
                wi = wi * wpr + wTemp * wpi
            }

            dftSize = dftSize shl 1
            t++
        }

        // 3. Magnitude & Log-equalize
        val limit = min(outSpectralData.size, equalize.size)
        for (i in 0 until limit) {
            outSpectralData[i] = sqrt(real[i] * real[i] + imag[i] * imag[i]) * equalize[i]
        }
    }
}

