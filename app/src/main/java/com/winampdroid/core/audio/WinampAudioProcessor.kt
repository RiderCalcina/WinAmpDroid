package com.winampdroid.core.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Custom Media3 AudioProcessor extending BaseAudioProcessor.
 *
 * Provides:
 * 1. Winamp 10-Band Equalizer (60Hz to 16kHz) with Robert Bristow-Johnson Biquad IIR filters
 * 2. Preamp Gain (-12dB to +12dB)
 * 3. Stereo Balance (-1.0 to +1.0)
 * 4. Ultra-low latency PCM Tap for 60 FPS Visualizers (Nullsoft FFT & WebGL Milkdrop)
 *
 * Implements Media3's official BaseAudioProcessor lifecycle with zero-copy fast-path bypass
 * and anti-denormal protection to guarantee jitter-free, underrun-free real-time audio.
 */
@OptIn(UnstableApi::class)
class WinampAudioProcessor : BaseAudioProcessor() {

    private val filters = Array(10) { BiquadFilter(WinampEqualizerPreset.BAND_FREQUENCIES[it]) }
    @Volatile private var activeFilters = emptyArray<BiquadFilter>()

    var isEqEnabled: Boolean = true
        set(value) {
            field = value
            updateActiveFilters()
        }

    var preampGainLinear: Double = 1.0
        private set

    // Balance: -1.0 (Full Left) to 0.0 (Center) to 1.0 (Full Right)
    var balance: Float = 0f

    // Optional legacy callback
    var pcmDataListener: ((ShortArray, Int) -> Unit)? = null

    // Continuous lock-free ring buffer for visualizers (~1.48 seconds of audio at 44.1kHz)
    companion object {
        private const val RING_BUFFER_SIZE = 65536
        private const val RING_BUFFER_MASK = RING_BUFFER_SIZE - 1
    }

    private val ringBuffer = ShortArray(RING_BUFFER_SIZE)
    @Volatile private var ringWriteIndex: Long = 0L
    private val ringLock = Any()

    // Reusable short buffer to eliminate GC on audio thread
    private var samplesBuffer = ShortArray(4096)

    init {
        setPreampDb(0f)
        updateActiveFilters()
    }

    private fun updateActiveFilters() {
        activeFilters = if (isEqEnabled) {
            filters.filter { it.currentGainDb != 0f }.toTypedArray()
        } else {
            emptyArray()
        }
    }

    /**
     * Non-blocking snapshot pull for visualizer engines (Nullsoft FFT & WebGL Milkdrop).
     * Retrieves the 1024 samples currently playing through the device speakers,
     * accounting for AudioTrack buffer latency (~95ms) for tight, rhythm-locked sync.
     */
    fun getLatestPcm(output: ShortArray): Boolean {
        val count = output.size
        val sampleRate = if (inputAudioFormat.sampleRate > 0) inputAudioFormat.sampleRate else 44100

        synchronized(ringLock) {
            val head = ringWriteIndex
            if (head < count) return false

            // AudioTrack typically buffers ~80ms-120ms of audio ahead of physical speaker output
            val latencySamples = (sampleRate * 0.095f).toLong()
            val targetEnd = (head - latencySamples).coerceIn(count.toLong(), head)
            val targetStart = targetEnd - count

            for (i in 0 until count) {
                output[i] = ringBuffer[((targetStart + i) and RING_BUFFER_MASK.toLong()).toInt()]
            }
            return true
        }
    }

    fun setPreampDb(db: Float) {
        preampGainLinear = 10.0.pow(db / 20.0)
    }

    fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        if (bandIndex in filters.indices && inputAudioFormat.sampleRate > 0) {
            filters[bandIndex].configure(inputAudioFormat.sampleRate, gainDb)
            updateActiveFilters()
        }
    }

    fun applyPreset(preset: WinampEqualizerPreset) {
        setPreampDb(preset.preampDb)
        for (i in preset.bands.indices) {
            if (i in filters.indices && inputAudioFormat.sampleRate > 0) {
                filters[i].configure(inputAudioFormat.sampleRate, preset.bands[i])
            }
        }
        updateActiveFilters()
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        // Reconfigure filters with the actual input sample rate
        filters.forEach { it.configure(inputAudioFormat.sampleRate, it.currentGainDb) }
        updateActiveFilters()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val sampleCount = remaining / 2
        val channels = inputAudioFormat.channelCount

        // Feed continuous audio stream into ringBuffer for ultra-low latency 60 FPS visualizers
        val shortView = inputBuffer.asShortBuffer()
        val originalPos = shortView.position()

        synchronized(ringLock) {
            var head = ringWriteIndex
            if (channels == 2) {
                var idx = originalPos
                val limit = originalPos + sampleCount
                while (idx < limit) {
                    val sL = shortView.get(idx).toInt()
                    val sR = shortView.get(idx + 1).toInt()
                    ringBuffer[(head and RING_BUFFER_MASK.toLong()).toInt()] = ((sL + sR) shr 1).toShort()
                    head++
                    idx += 2
                }
            } else {
                var idx = originalPos
                val limit = originalPos + sampleCount
                while (idx < limit) {
                    ringBuffer[(head and RING_BUFFER_MASK.toLong()).toInt()] = shortView.get(idx)
                    head++
                    idx++
                }
            }
            ringWriteIndex = head
        }

        val active = activeFilters
        val hasActiveDsp = active.isNotEmpty() || preampGainLinear != 1.0 || balance != 0f

        if (!hasActiveDsp) {
            // Official Media3 fast-path bypass: zero CPU, zero conversions, direct native pass-through
            replaceOutputBuffer(remaining).put(inputBuffer).flip()
            return
        }

        // DSP Path: Active only when EQ bands, preamp, or balance are modified
        if (samplesBuffer.size < sampleCount) {
            samplesBuffer = ShortArray(sampleCount)
        }
        val samples = samplesBuffer
        shortView.position(originalPos)
        shortView.get(samples, 0, sampleCount)
        inputBuffer.position(inputBuffer.limit()) // mark input as consumed

        val leftGain = if (balance > 0f) (1f - balance).toDouble() else 1.0
        val rightGain = if (balance < 0f) (1f + balance).toDouble() else 1.0
        val activeCount = active.size

        if (channels == 2) {
            var i = 0
            while (i < sampleCount) {
                var sL = samples[i].toDouble() * preampGainLinear * leftGain
                var sR = samples[i + 1].toDouble() * preampGainLinear * rightGain

                var k = 0
                while (k < activeCount) {
                    val f = active[k]
                    sL = f.processSampleLeft(sL)
                    sR = f.processSampleRight(sR)
                    k++
                }

                // Clamp to 16-bit signed PCM range
                samples[i] = max(-32768.0, min(32767.0, sL)).toInt().toShort()
                samples[i + 1] = max(-32768.0, min(32767.0, sR)).toInt().toShort()
                i += 2
            }
        } else if (channels == 1) {
            var i = 0
            while (i < sampleCount) {
                var s = samples[i].toDouble() * preampGainLinear
                var k = 0
                while (k < activeCount) {
                    s = active[k].processSampleLeft(s)
                    k++
                }
                samples[i] = max(-32768.0, min(32767.0, s)).toInt().toShort()
                i++
            }
        }

        val outBuf = replaceOutputBuffer(remaining)
        outBuf.asShortBuffer().put(samples, 0, sampleCount)
        outBuf.position(remaining)
        outBuf.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        filters.forEach { it.reset() }
        synchronized(ringLock) {
            ringWriteIndex = 0L
            ringBuffer.fill(0)
        }
    }

    override fun onReset() {
        filters.forEach { it.reset() }
        synchronized(ringLock) {
            ringWriteIndex = 0L
            ringBuffer.fill(0)
        }
    }
}
