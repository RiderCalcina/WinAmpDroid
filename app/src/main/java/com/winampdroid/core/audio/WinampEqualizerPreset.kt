package com.winampdroid.core.audio

data class WinampEqualizerPreset(
    val name: String,
    val preampDb: Float = 0f,
    val bands: FloatArray = FloatArray(10) { 0f }
) {
    companion object {
        val BAND_FREQUENCIES = floatArrayOf(
            60f, 170f, 310f, 600f, 1000f, 3000f, 6000f, 12000f, 14000f, 16000f
        )

        val FLAT = WinampEqualizerPreset("Flat")

        val ROCK = WinampEqualizerPreset(
            name = "Rock",
            preampDb = 0f,
            bands = floatArrayOf(4.5f, 3.0f, -1.5f, -2.5f, -1.0f, 2.0f, 5.0f, 6.5f, 7.0f, 7.0f)
        )

        val TECHNO = WinampEqualizerPreset(
            name = "Techno",
            preampDb = 0f,
            bands = floatArrayOf(5.0f, 3.5f, 0f, -3.5f, -3.0f, 0f, 5.0f, 6.0f, 6.0f, 5.5f)
        )

        val FULL_BASS = WinampEqualizerPreset(
            name = "Full Bass",
            preampDb = 0f,
            bands = floatArrayOf(7.0f, 6.5f, 6.0f, 3.5f, 1.0f, -2.5f, -5.0f, -6.5f, -7.0f, -7.0f)
        )

        val CLASSICAL = WinampEqualizerPreset(
            name = "Classical",
            preampDb = 0f,
            bands = floatArrayOf(4.5f, 3.5f, 3.0f, 2.5f, -1.5f, -1.5f, 0f, 2.0f, 3.0f, 3.5f)
        )

        val CLUB = WinampEqualizerPreset(
            name = "Club",
            preampDb = 0f,
            bands = floatArrayOf(0f, 0f, 2.0f, 3.5f, 3.5f, 3.5f, 2.0f, 0f, 0f, 0f)
        )

        val DANCE = WinampEqualizerPreset(
            name = "Dance",
            preampDb = 0f,
            bands = floatArrayOf(7.0f, 5.5f, 2.0f, 0f, 0f, -3.0f, -4.5f, -4.5f, 0f, 0f)
        )

        val FULL_TREBLE = WinampEqualizerPreset(
            name = "Full Treble",
            preampDb = 0f,
            bands = floatArrayOf(-7.0f, -7.0f, -7.0f, -3.5f, 1.5f, 6.5f, 8.5f, 8.5f, 8.5f, 9.0f)
        )

        val POP = WinampEqualizerPreset(
            name = "Pop",
            preampDb = 0f,
            bands = floatArrayOf(-1.5f, 1.0f, 4.5f, 5.0f, 3.5f, -1.0f, -2.0f, -2.0f, -1.5f, -1.5f)
        )

        val REGGAE = WinampEqualizerPreset(
            name = "Reggae",
            preampDb = 0f,
            bands = floatArrayOf(0f, 0f, -0.5f, -4.0f, 0f, 4.0f, 4.0f, 0.5f, 0f, 0f)
        )

        val SKA = WinampEqualizerPreset(
            name = "Ska",
            preampDb = 0f,
            bands = floatArrayOf(-2.0f, -3.5f, -3.0f, -0.5f, 2.5f, 3.5f, 6.5f, 7.0f, 7.5f, 7.0f)
        )

        val SOFT = WinampEqualizerPreset(
            name = "Soft",
            preampDb = 0f,
            bands = floatArrayOf(3.5f, 1.0f, -1.0f, -1.5f, -0.5f, 2.5f, 5.5f, 7.0f, 8.0f, 8.5f)
        )

        val VOCAL = WinampEqualizerPreset(
            name = "Vocal",
            preampDb = 0f,
            bands = floatArrayOf(-1.5f, -3.5f, -3.5f, 1.0f, 4.0f, 4.0f, 3.0f, 1.5f, 0f, -1.5f)
        )

        val DEFAULT_PRESETS = listOf(
            FLAT,
            ROCK,
            TECHNO,
            FULL_BASS,
            CLASSICAL,
            CLUB,
            DANCE,
            FULL_TREBLE,
            POP,
            REGGAE,
            SKA,
            SOFT,
            VOCAL
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WinampEqualizerPreset
        if (name != other.name) return false
        if (preampDb != other.preampDb) return false
        if (!bands.contentEquals(other.bands)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + preampDb.hashCode()
        result = 31 * result + bands.contentHashCode()
        return result
    }
}
