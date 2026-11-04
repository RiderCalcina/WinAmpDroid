package com.winampdroid.core.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.winampdroid.core.model.Track

object MediaStoreScanner {

    fun scanDeviceAudio(context: Context): List<Track> {
        val tracks = mutableListOf<Track>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Canción"
                    val artist = cursor.getString(artistCol) ?: "Artista Desconocido"
                    val durMs = cursor.getLong(durCol)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    // Skip very short sounds (< 5s ringtones/notifications)
                    if (durMs >= 5000) {
                        tracks.add(
                            Track(
                                id = "device_$id",
                                title = title,
                                artist = if (artist.contains("<unknown>", ignoreCase = true)) "Desconocido" else artist,
                                durationSeconds = durMs / 1000.0,
                                uri = contentUri,
                                isAsset = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tracks
    }
}

