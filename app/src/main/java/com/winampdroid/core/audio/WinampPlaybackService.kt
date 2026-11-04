package com.winampdroid.core.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.winampdroid.MainActivity
import com.winampdroid.R
import com.winampdroid.core.model.Track
import com.winampdroid.ui.widget.WinampAppWidgetProvider

@OptIn(UnstableApi::class)
class WinampPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    val audioProcessor = WinampAudioProcessor()
    private val binder = LocalBinder()

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    var currentTrackTitle = "WinAmpDroid"
        private set

    var onMetadataListener: ((String) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): WinampPlaybackService = this@WinampPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initLocks()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(audioProcessor))
                    .build()
            }
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    acquireLocks()
                    updateForegroundNotification(currentTrackTitle, true)
                } else {
                    releaseLocks()
                    updateForegroundNotification(currentTrackTitle, false)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentTrackTitle = mediaItem?.mediaMetadata?.title?.toString()
                    ?: mediaItem?.mediaId
                    ?: "WinAmpDroid"
                updateForegroundNotification(currentTrackTitle, player.isPlaying)
                onMetadataListener?.invoke(currentTrackTitle)
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                val streamTitle = mediaMetadata.title?.toString()
                    ?: mediaMetadata.displayTitle?.toString()
                if (!streamTitle.isNullOrBlank() && streamTitle != currentTrackTitle) {
                    currentTrackTitle = streamTitle
                    updateForegroundNotification(currentTrackTitle, player.isPlaying)
                    onMetadataListener?.invoke(streamTitle)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                error.printStackTrace()
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.prepare()
                    player.play()
                }
            }
            ACTION_PREV -> {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else {
                    player.seekTo(0)
                }
                player.prepare()
                player.play()
            }
            ACTION_NEXT -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                }
                player.prepare()
                player.play()
            }
            ACTION_STOP -> {
                player.pause()
                player.seekTo(0)
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        updateForegroundNotification(currentTrackTitle, player.isPlaying)
        return super.onStartCommand(intent, flags, startId)
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int, playNow: Boolean = true) {
        if (tracks.isEmpty()) {
            clearPlaylist()
            return
        }
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setDisplayTitle("${track.artist} - ${track.title}")
                        .build()
                )
                .build()
        }
        val safeIndex = startIndex.coerceIn(0, items.size - 1)
        player.setMediaItems(items, safeIndex, 0L)
        player.prepare()
        currentTrackTitle = tracks.getOrNull(safeIndex)?.let { "${it.artist} - ${it.title}" } ?: "WinAmpDroid"
        updateForegroundNotification(currentTrackTitle, playNow)
        onMetadataListener?.invoke(currentTrackTitle)
        if (playNow) {
            player.play()
        }
    }

    fun clearPlaylist() {
        try {
            player.stop()
            player.clearMediaItems()
            releaseLocks()
            currentTrackTitle = "WINAMP"
            onMetadataListener?.invoke("WINAMP")
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WinAmpDroid:PlaybackWakeLock")

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "WinAmpDroid:StreamWifiLock")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld == false) wakeLock?.acquire(24 * 60 * 60 * 1000L)
            if (wifiLock?.isHeld == false) wifiLock?.acquire()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateForegroundNotification(title: String, isPlaying: Boolean) {
        val notification = buildNotification(title, isPlaying)
        try {
            WinampAppWidgetProvider.updateAllWidgets(this, title, isPlaying)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(title: String, isPlaying: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WinampPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, WinampPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, WinampPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (isPlaying) "Pausar" else "Reproducir"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Winamp - $title")
            .setContentText(if (isPlaying) "Reproduciendo audio..." else "En pausa")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(openAppIntent)
            .setOngoing(isPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", prevIntent)
            .addAction(playPauseIcon, playPauseText, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Siguiente", nextIntent)

        mediaSession?.let { session ->
            builder.setStyle(
                MediaStyleNotificationHelper.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == "androidx.media3.session.MediaSessionService") {
            return super.onBind(intent)
        }
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        releaseLocks()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "winamp_playback_channel"
        const val NOTIFICATION_ID = 1998 // Winamp release year
        const val ACTION_PLAY_PAUSE = "com.winampdroid.action.PLAY_PAUSE"
        const val ACTION_PREV = "com.winampdroid.action.PREV"
        const val ACTION_NEXT = "com.winampdroid.action.NEXT"
        const val ACTION_STOP = "com.winampdroid.action.STOP"
    }
}
