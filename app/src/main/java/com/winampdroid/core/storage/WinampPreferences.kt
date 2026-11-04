package com.winampdroid.core.storage

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.winampdroid.core.audio.WinampEqualizerPreset
import com.winampdroid.core.model.Track

data class TrackDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Double = 0.0,
    val uriString: String,
    val isAsset: Boolean = false
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        uri = Uri.parse(uriString),
        isAsset = isAsset
    )

    companion object {
        fun fromTrack(track: Track): TrackDto = TrackDto(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds,
            uriString = track.uri.toString(),
            isAsset = track.isAsset
        )
    }
}

data class WindowStates(
    val isEqOpen: Boolean = true,
    val isPlOpen: Boolean = true,
    val isMilkdropOpen: Boolean = true,
    val isMainShade: Boolean = false,
    val isEqShade: Boolean = false,
    val isPlShade: Boolean = false,
    val isMilkdropShade: Boolean = false
)

class WinampPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("winamp_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlaylist(tracks: List<Track>, currentIndex: Int) {
        val dtos = tracks.map { TrackDto.fromTrack(it) }
        val json = gson.toJson(dtos)
        prefs.edit()
            .putString("playlist_json", json)
            .putInt("current_track_index", currentIndex)
            .apply()
    }

    fun loadPlaylist(): Pair<List<Track>, Int>? {
        val json = prefs.getString("playlist_json", null) ?: return null
        val currentIndex = prefs.getInt("current_track_index", 0)
        return try {
            val type = object : TypeToken<List<TrackDto>>() {}.type
            val dtos: List<TrackDto> = gson.fromJson(json, type)
            Pair(dtos.map { it.toTrack() }, currentIndex)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveAudioSettings(
        volume: Float,
        balance: Float,
        preampDb: Float,
        bandsDb: FloatArray,
        isEqEnabled: Boolean,
        isShuffle: Boolean,
        isRepeat: Boolean
    ) {
        val bandsJson = gson.toJson(bandsDb)
        prefs.edit()
            .putFloat("volume", volume)
            .putFloat("balance", balance)
            .putFloat("preamp_db", preampDb)
            .putString("bands_db_json", bandsJson)
            .putBoolean("is_eq_enabled", isEqEnabled)
            .putBoolean("is_shuffle", isShuffle)
            .putBoolean("is_repeat", isRepeat)
            .apply()
    }

    fun getVolume(default: Float = 0.8f): Float = prefs.getFloat("volume", default)
    fun getBalance(default: Float = 0f): Float = prefs.getFloat("balance", default)
    fun getPreampDb(default: Float = 0f): Float = prefs.getFloat("preamp_db", default)
    fun getIsEqEnabled(default: Boolean = true): Boolean = prefs.getBoolean("is_eq_enabled", default)
    fun getIsShuffle(default: Boolean = false): Boolean = prefs.getBoolean("is_shuffle", default)
    fun getIsRepeat(default: Boolean = false): Boolean = prefs.getBoolean("is_repeat", default)

    fun getBandsDb(): FloatArray {
        val json = prefs.getString("bands_db_json", null) ?: return FloatArray(10) { 0f }
        return try {
            gson.fromJson(json, FloatArray::class.java)
        } catch (_: Exception) {
            FloatArray(10) { 0f }
        }
    }

    fun saveCustomEqualizerPreset(preset: WinampEqualizerPreset) {
        val currentList = loadCustomEqualizerPresets().toMutableList()
        currentList.removeAll { it.name.equals(preset.name, ignoreCase = true) }
        currentList.add(preset)
        val json = gson.toJson(currentList)
        prefs.edit().putString("custom_eq_presets_json", json).apply()
    }

    fun loadCustomEqualizerPresets(): List<WinampEqualizerPreset> {
        val json = prefs.getString("custom_eq_presets_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WinampEqualizerPreset>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteCustomEqualizerPreset(name: String) {
        val currentList = loadCustomEqualizerPresets().toMutableList()
        currentList.removeAll { it.name.equals(name, ignoreCase = true) }
        val json = gson.toJson(currentList)
        prefs.edit().putString("custom_eq_presets_json", json).apply()
    }

    fun saveWindowVisibility(isEqOpen: Boolean, isPlOpen: Boolean) {
        prefs.edit()
            .putBoolean("is_eq_open", isEqOpen)
            .putBoolean("is_pl_open", isPlOpen)
            .apply()
    }

    fun getIsEqOpen(default: Boolean = true): Boolean = prefs.getBoolean("is_eq_open", default)
    fun getIsPlOpen(default: Boolean = true): Boolean = prefs.getBoolean("is_pl_open", default)
    fun getIsMilkdropOpen(default: Boolean = true): Boolean = prefs.getBoolean("is_milkdrop_open", default)

    fun saveWindowStates(states: WindowStates) {
        prefs.edit()
            .putBoolean("is_eq_open", states.isEqOpen)
            .putBoolean("is_pl_open", states.isPlOpen)
            .putBoolean("is_milkdrop_open", states.isMilkdropOpen)
            .putBoolean("is_main_shade", states.isMainShade)
            .putBoolean("is_eq_shade", states.isEqShade)
            .putBoolean("is_pl_shade", states.isPlShade)
            .putBoolean("is_milkdrop_shade", states.isMilkdropShade)
            .apply()
    }

    fun loadWindowStates(): WindowStates {
        return WindowStates(
            isEqOpen = prefs.getBoolean("is_eq_open", true),
            isPlOpen = prefs.getBoolean("is_pl_open", true),
            isMilkdropOpen = prefs.getBoolean("is_milkdrop_open", true),
            isMainShade = prefs.getBoolean("is_main_shade", false),
            isEqShade = prefs.getBoolean("is_eq_shade", false),
            isPlShade = prefs.getBoolean("is_pl_shade", false),
            isMilkdropShade = prefs.getBoolean("is_milkdrop_shade", false)
        )
    }

    fun saveActiveSkin(skinPath: String) {
        prefs.edit().putString("active_skin_path", skinPath).apply()
    }

    fun getActiveSkin(default: String = "skins/base.wsz"): String =
        prefs.getString("active_skin_path", default) ?: default

    fun saveDownloadedSkin(title: String, filePath: String, md5: String) {
        val list = getDownloadedSkins().toMutableList()
        list.removeAll { it.md5 == md5 || it.filePath == filePath }
        list.add(0, DownloadedSkin(title, filePath, md5))
        val json = gson.toJson(list)
        prefs.edit().putString("downloaded_skins_json", json).apply()
    }

    fun getDownloadedSkins(): List<DownloadedSkin> {
        val json = prefs.getString("downloaded_skins_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<DownloadedSkin>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun deleteDownloadedSkin(md5: String) {
        val list = getDownloadedSkins().toMutableList()
        list.removeAll { it.md5 == md5 }
        val json = gson.toJson(list)
        prefs.edit().putString("downloaded_skins_json", json).apply()
    }
}

data class DownloadedSkin(
    val title: String,
    val filePath: String,
    val md5: String
)
