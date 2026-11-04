package com.winampdroid

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.winampdroid.core.audio.WinampPlaybackService
import com.winampdroid.core.media.MediaStoreScanner
import com.winampdroid.core.model.Track
import com.winampdroid.core.skin.WinampSkin
import com.winampdroid.core.skin.WszSkinParser
import com.winampdroid.core.storage.DownloadedSkin
import com.winampdroid.core.storage.WinampPreferences
import com.winampdroid.core.visualizer.NullsoftFFT
import com.winampdroid.ui.desktop.RetroDesktopLayout
import com.winampdroid.ui.desktop.components.SkinMuseumDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

@OptIn(UnstableApi::class)
class MainActivity : ComponentActivity() {

    private var playbackService by mutableStateOf<WinampPlaybackService?>(null)
    private var isBound by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && playbackService?.player?.isPlaying == true) {
            enterPipMode()
        }
    }

    private var onAudioFilesSelected: ((List<Uri>) -> Unit)? = null
    private var onSkinFileSelected: ((Uri) -> Unit)? = null
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    // Audio Files Picker (Multiple tracks with persistable URI permissions)
    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            for (uri in uris) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
            onAudioFilesSelected?.invoke(uris)
        }
    }

    // Skin (.wsz) File Picker
    private val skinPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSkinFileSelected?.invoke(uri)
        }
    }

    // Media permissions launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WinampPlaybackService.LocalBinder
            playbackService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for background audio playback on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Start & bind Playback Service as Foreground Service for background audio survival
        val serviceIntent = Intent(this, WinampPlaybackService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        val prefs = WinampPreferences(this)

        // Built-in skins catalog: solo Winamp 2.91 Clásico Original
        val builtInSkins = listOf(
            Pair("Winamp 2.91 Clásico Original", "skins/base.wsz")
        )

        val initialSkinPath = prefs.getActiveSkin()
        val initialSkin: WinampSkin? = if (initialSkinPath.startsWith("/")) {
            try {
                val skinFile = java.io.File(initialSkinPath)
                if (skinFile.exists()) {
                    skinFile.inputStream().use { WszSkinParser.parse(it) }
                } else null
            } catch (e: Exception) {
                null
            } ?: loadAssetSkin("skins/base.wsz")
        } else {
            loadAssetSkin(initialSkinPath) ?: loadAssetSkin("skins/base.wsz")
        }

        // Default tracks: DJ Mike Llama + Rock Antenne streams
        val defaultLlamaTrack = Track(
            id = "demo_llama",
            title = "Llama Whippin' Intro",
            artist = "DJ Mike Llama",
            durationSeconds = 5.0,
            uri = Uri.parse("asset:///audio/llama.mp3"),
            isAsset = true
        )
        val rockAntenneTrack = Track(
            id = "stream_rock_antenne",
            title = "the best music in the world",
            artist = "Rock Antenne",
            durationSeconds = 0.0,
            uri = Uri.parse("https://stream.rockantenne.de/rockantenne/stream/mp3"),
            isAsset = false
        )
        val heavyMetalTrack = Track(
            id = "stream_heavy_metal",
            title = "the best music in the world",
            artist = "Heavy Metal",
            durationSeconds = 0.0,
            uri = Uri.parse("https://s1-webradio.rockantenne.de/heavy-metal/stream/mp3"),
            isAsset = false
        )
        val defaultTracksList = listOf(defaultLlamaTrack, rockAntenneTrack, heavyMetalTrack)

        val loadedData = prefs.loadPlaylist()
        val initialTracks = if (loadedData != null && loadedData.first.isNotEmpty()) {
            loadedData.first
        } else {
            defaultTracksList
        }
        val initialIndex = (loadedData?.second ?: 0).coerceIn(0, maxOf(0, initialTracks.size - 1))

        setContent {
            var skin by remember { mutableStateOf(initialSkin) }
            val downloadedSkins = remember { mutableStateListOf<DownloadedSkin>().apply { addAll(prefs.getDownloadedSkins()) } }
            val tracks = remember { mutableStateListOf<Track>().apply { addAll(initialTracks) } }
            var currentTrackIndex by remember { mutableIntStateOf(initialIndex) }

            var isPlaying by remember { mutableStateOf(false) }
            var isUserPaused by remember { mutableStateOf(false) }
            val isPaused = isUserPaused && !isPlaying
            var timeElapsedSeconds by remember { mutableIntStateOf(0) }
            var totalDurationSeconds by remember { mutableIntStateOf(5) }

            var volume by remember { mutableFloatStateOf(prefs.getVolume()) }
            var balance by remember { mutableFloatStateOf(prefs.getBalance()) }

            var isShuffle by remember { mutableStateOf(prefs.getIsShuffle()) }
            var isRepeat by remember { mutableStateOf(prefs.getIsRepeat()) }

            val initialWindowStates = remember { prefs.loadWindowStates() }
            var isEqOpen by remember { mutableStateOf(initialWindowStates.isEqOpen) }
            var isPlOpen by remember { mutableStateOf(initialWindowStates.isPlOpen) }
            var isMilkdropOpen by remember { mutableStateOf(initialWindowStates.isMilkdropOpen) }

            var isMainShade by remember { mutableStateOf(initialWindowStates.isMainShade) }
            var isEqShade by remember { mutableStateOf(initialWindowStates.isEqShade) }
            var isPlShade by remember { mutableStateOf(initialWindowStates.isPlShade) }
            var isMilkdropShade by remember { mutableStateOf(initialWindowStates.isMilkdropShade) }

            var isEqEnabled by remember { mutableStateOf(prefs.getIsEqEnabled()) }
            var preampDb by remember { mutableFloatStateOf(prefs.getPreampDb()) }
            val bandsDb = remember { mutableStateOf(prefs.getBandsDb()) }
            val customPresets = remember { mutableStateListOf<com.winampdroid.core.audio.WinampEqualizerPreset>().apply { addAll(prefs.loadCustomEqualizerPresets()) } }

            val visualizerDataState = remember { mutableStateOf(com.winampdroid.ui.desktop.VisualizerData()) }
            val fft = remember { NullsoftFFT() }
            val floatPcmBuffer = remember { FloatArray(1024) { 0f } }
            val spectralBuffer = remember { FloatArray(512) { 0f } }
            val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
            val milkdropBridge = remember { com.winampdroid.ui.desktop.components.MilkdropBridge() }
            var liveStreamMetadataTitle by remember { mutableStateOf<String?>(null) }

            var showSkinMuseumDialog by remember { mutableStateOf(false) }

            fun saveAllSettings() {
                prefs.saveAudioSettings(
                    volume = volume,
                    balance = balance,
                    preampDb = preampDb,
                    bandsDb = bandsDb.value,
                    isEqEnabled = isEqEnabled,
                    isShuffle = isShuffle,
                    isRepeat = isRepeat
                )
                prefs.savePlaylist(tracks, currentTrackIndex)
                prefs.saveWindowStates(
                    com.winampdroid.core.storage.WindowStates(
                        isEqOpen = isEqOpen,
                        isPlOpen = isPlOpen,
                        isMilkdropOpen = isMilkdropOpen,
                        isMainShade = isMainShade,
                        isEqShade = isEqShade,
                        isPlShade = isPlShade,
                        isMilkdropShade = isMilkdropShade
                    )
                )
            }

            // Audio files picker callback (offloaded to IO thread)
            onAudioFilesSelected = { uris ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val newTracks = uris.map { uri -> extractTrackMetadata(uri) }
                    withContext(Dispatchers.Main) {
                        tracks.addAll(newTracks)
                        playbackService?.setPlaylist(tracks, currentTrackIndex, playNow = isPlaying)
                        prefs.savePlaylist(tracks, currentTrackIndex)
                        Toast.makeText(this@MainActivity, "Se añadieron ${newTracks.size} canciones", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Custom skin picker callback (offloaded to IO thread)
            onSkinFileSelected = { uri ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val parsed = WszSkinParser.parse(stream)
                            withContext(Dispatchers.Main) {
                                skin = parsed
                                Toast.makeText(this@MainActivity, "Skin cargada con éxito", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Error al cargar la skin: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            // Function to trigger MediaStore scanning on background thread
            fun requestScanDeviceAudio() {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    android.Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }

                if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val scanned = MediaStoreScanner.scanDeviceAudio(this@MainActivity)
                        withContext(Dispatchers.Main) {
                            if (scanned.isNotEmpty()) {
                                tracks.addAll(scanned)
                                playbackService?.setPlaylist(tracks, currentTrackIndex, playNow = isPlaying)
                                prefs.savePlaylist(tracks, currentTrackIndex)
                                Toast.makeText(this@MainActivity, "Se encontraron ${scanned.size} canciones", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "No se encontraron canciones en el dispositivo", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    onPermissionResult = { granted ->
                        if (granted) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val scanned = MediaStoreScanner.scanDeviceAudio(this@MainActivity)
                                withContext(Dispatchers.Main) {
                                    if (scanned.isNotEmpty()) {
                                        tracks.addAll(scanned)
                                        playbackService?.setPlaylist(tracks, currentTrackIndex, playNow = isPlaying)
                                        prefs.savePlaylist(tracks, currentTrackIndex)
                                        Toast.makeText(this@MainActivity, "Se encontraron ${scanned.size} canciones", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Permiso denegado para escanear audio", Toast.LENGTH_SHORT).show()
                        }
                    }
                    permissionLauncher.launch(perm)
                }
            }

            // Auto-advance & Playback state listener
            LaunchedEffect(isBound) {
                playbackService?.player?.let { player ->
                    // Apply restored settings
                    player.volume = volume
                    player.shuffleModeEnabled = isShuffle
                    player.repeatMode = if (isRepeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF

                    playbackService?.audioProcessor?.let { proc ->
                        proc.isEqEnabled = isEqEnabled
                        proc.balance = balance
                        proc.setPreampDb(preampDb)
                        bandsDb.value.forEachIndexed { i, gain -> proc.setBandGainDb(i, gain) }
                    }

                    if (tracks.isEmpty()) {
                        playbackService?.clearPlaylist()
                    } else if (player.mediaItemCount == 0) {
                        playbackService?.setPlaylist(tracks, currentTrackIndex, playNow = false)
                    }

                    player.addListener(object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            val id = mediaItem?.mediaId
                            val matchedIdx = if (id != null) tracks.indexOfFirst { it.id == id } else -1
                            val newIdx = if (matchedIdx >= 0) matchedIdx else player.currentMediaItemIndex
                            if (newIdx in tracks.indices && newIdx != currentTrackIndex) {
                                currentTrackIndex = newIdx
                                prefs.savePlaylist(tracks, currentTrackIndex)
                            }
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                if (isRepeat && tracks.isNotEmpty()) {
                                    currentTrackIndex = 0
                                    playTrackAtIndex(tracks, 0)
                                }
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            Toast.makeText(this@MainActivity, "Error al reproducir: ${error.errorCodeName}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                while (true) {
                    playbackService?.player?.let { player ->
                        val pPlaying = player.isPlaying
                        if (isPlaying != pPlaying) {
                            isPlaying = pPlaying
                        }
                        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                            isUserPaused = false
                        }
                        val posSec = (player.currentPosition / 1000).toInt()
                        if (posSec != timeElapsedSeconds) {
                            timeElapsedSeconds = posSec
                        }
                        val dur = player.duration
                        val durSec = if (dur > 0) (dur / 1000).toInt() else 0
                        if (durSec != totalDurationSeconds) {
                            totalDurationSeconds = durSec
                        }
                    }
                    delay(250)
                }
            }

            // High-Performance Audio Visualizer Polling (Decoupled on Dispatchers.Default at 60 FPS)
            LaunchedEffect(playbackService, isPlaying) {
                if (!isPlaying) {
                    visualizerDataState.value = com.winampdroid.ui.desktop.VisualizerData()
                    return@LaunchedEffect
                }
                withContext(Dispatchers.Default) {
                    val localPcm = ShortArray(1024)
                    while (isActive && isPlaying) {
                        val hasData = playbackService?.audioProcessor?.getLatestPcm(localPcm) == true
                        if (hasData) {
                            for (i in 0 until 1024) {
                                floatPcmBuffer[i] = localPcm[i] / 32768f
                            }
                            fft.timeToFrequencyDomain(floatPcmBuffer, spectralBuffer)

                            // Calibrated dynamic bands for WebGL Milkdrop shader (0.0 to 1.0)
                            var bSum = 0f
                            val bMax = minOf(16, spectralBuffer.size)
                            for (i in 0 until bMax) bSum += spectralBuffer[i]
                            val b = (bSum / (bMax * 8f)).coerceIn(0f, 1f)

                            var mSum = 0f
                            val mMax = minOf(64, spectralBuffer.size)
                            val mCount = (mMax - bMax).coerceAtLeast(1)
                            for (i in bMax until mMax) mSum += spectralBuffer[i]
                            val m = (mSum / (mCount * 6f)).coerceIn(0f, 1f)

                            var tSum = 0f
                            val tMax = minOf(128, spectralBuffer.size)
                            val tCount = (tMax - mMax).coerceAtLeast(1)
                            for (i in mMax until tMax) tSum += spectralBuffer[i]
                            val t = (tSum / (tCount * 4f)).coerceIn(0f, 1f)

                            milkdropBridge.updateAudio(b, m, t)

                            val pcmCopy = localPcm.copyOf()
                            val specCopy = spectralBuffer.copyOf()
                            mainHandler.post {
                                visualizerDataState.value = com.winampdroid.ui.desktop.VisualizerData(
                                    spectralData = specCopy,
                                    pcmWaveData = pcmCopy
                                )
                            }
                        }
                        delay(16L) // 60 FPS ultra-smooth, real-time beat sync
                    }
                }
            }

            // Live stream metadata listener
            LaunchedEffect(playbackService) {
                playbackService?.onMetadataListener = { liveTitle ->
                    liveStreamMetadataTitle = liveTitle
                }
            }

            LaunchedEffect(isPlaying, isPaused) {
                milkdropBridge.updateState(isPlaying, isPaused)
                if (!isPlaying || isPaused) {
                    visualizerDataState.value = com.winampdroid.ui.desktop.VisualizerData()
                }
            }

            val playAction: () -> Unit = {
                if (tracks.isEmpty()) {
                    Toast.makeText(this@MainActivity, "La lista está vacía", Toast.LENGTH_SHORT).show()
                } else {
                    isUserPaused = false
                    playbackService?.let { service ->
                        if (service.player.playbackState == Player.STATE_IDLE || 
                            service.player.playbackState == Player.STATE_ENDED || 
                            service.player.mediaItemCount == 0) {
                            playTrackAtIndex(tracks, currentTrackIndex)
                        } else {
                            service.player.prepare()
                            service.player.play()
                        }
                    }
                }
            }

            val pauseAction: () -> Unit = {
                playbackService?.player?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        isUserPaused = true
                    } else if (isUserPaused) {
                        player.play()
                        isUserPaused = false
                    }
                }
            }

            val stopAction: () -> Unit = {
                isUserPaused = false
                playbackService?.player?.let { player ->
                    player.pause()
                    player.seekTo(0)
                }
                timeElapsedSeconds = 0
                isPlaying = false
                liveStreamMetadataTitle = null
            }

            val prevAction: () -> Unit = {
                if (tracks.isNotEmpty()) {
                    isUserPaused = false
                    liveStreamMetadataTitle = null
                    playbackService?.player?.let { player ->
                        if (player.currentPosition > 3000) {
                            player.seekTo(0)
                            player.prepare()
                            player.play()
                        } else if (player.hasPreviousMediaItem()) {
                            player.seekToPreviousMediaItem()
                            player.prepare()
                            player.play()
                        } else if (tracks.isNotEmpty()) {
                            val newIdx = if (isShuffle && tracks.size > 1) {
                                val candidates = tracks.indices.filter { it != currentTrackIndex }
                                candidates.random()
                            } else if (currentTrackIndex > 0) {
                                currentTrackIndex - 1
                            } else if (isRepeat) {
                                tracks.size - 1
                            } else {
                                0
                            }
                            currentTrackIndex = newIdx
                            playTrackAtIndex(tracks, newIdx)
                        }
                    }
                }
            }

            val nextAction: () -> Unit = {
                if (tracks.isNotEmpty()) {
                    isUserPaused = false
                    liveStreamMetadataTitle = null
                    playbackService?.player?.let { player ->
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                            player.prepare()
                            player.play()
                        } else if (tracks.isNotEmpty()) {
                            val newIdx = if (isShuffle && tracks.size > 1) {
                                val candidates = tracks.indices.filter { it != currentTrackIndex }
                                candidates.random()
                            } else if (currentTrackIndex < tracks.size - 1) {
                                currentTrackIndex + 1
                            } else if (isRepeat) {
                                0
                            } else {
                                currentTrackIndex
                            }
                            currentTrackIndex = newIdx
                            playTrackAtIndex(tracks, newIdx)
                        }
                    }
                }
            }

            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            if (isInPipMode) {
                val currentTrack = tracks.getOrNull(currentTrackIndex)
                val trackTitle = liveStreamMetadataTitle ?: currentTrack?.let { "${it.artist} - ${it.title}" } ?: "WinAmpDroid"
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black)
                ) {
                    com.winampdroid.ui.desktop.components.MilkdropWindowView(
                        skin = skin,
                        trackTitle = trackTitle,
                        isPlaying = isPlaying,
                        isPaused = isPaused,
                        visualizerDataProvider = { visualizerDataState.value },
                        milkdropBridge = milkdropBridge,
                        onClose = {},
                        isShade = false,
                        onEnterPip = null,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "⚡ $trackTitle",
                        color = androidx.compose.ui.graphics.Color(0xFF00FF44),
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(androidx.compose.ui.graphics.Color(0x99000000))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (isLandscape) {
                // Modo Horizontal: Pantalla completa panorámica WebGL Milkdrop con HUD flotante auto-ocultable y controles retro
                val currentTrack = tracks.getOrNull(currentTrackIndex)
                val trackTitle = liveStreamMetadataTitle ?: currentTrack?.let { "${it.artist} - ${it.title}" } ?: "WinAmpDroid"
                com.winampdroid.ui.desktop.components.LandscapeMilkdropLayout(
                    trackTitle = trackTitle,
                    isPlaying = isPlaying,
                    isPaused = isPaused,
                    timeElapsedSeconds = timeElapsedSeconds,
                    totalDurationSeconds = totalDurationSeconds,
                    milkdropBridge = milkdropBridge,
                    onPlay = playAction,
                    onPause = pauseAction,
                    onStop = stopAction,
                    onPrev = prevAction,
                    onNext = nextAction,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RetroDesktopLayout(
                    skin = skin,
                    tracks = tracks,
                    currentTrackIndex = currentTrackIndex,
                    displayTitleOverride = liveStreamMetadataTitle,
                isPlaying = isPlaying,
                isPaused = isPaused,
                timeElapsedSeconds = timeElapsedSeconds,
                totalDurationSeconds = totalDurationSeconds,
                volume = volume,
                balance = balance,
                isShuffle = isShuffle,
                isRepeat = isRepeat,
                isEqOpen = isEqOpen,
                isPlOpen = isPlOpen,
                isMilkdropOpen = isMilkdropOpen,
                isMainShade = isMainShade,
                isEqShade = isEqShade,
                isPlShade = isPlShade,
                isMilkdropShade = isMilkdropShade,
                onToggleMainShade = {
                    isMainShade = !isMainShade
                    saveAllSettings()
                },
                onToggleEqShade = {
                    isEqShade = !isEqShade
                    saveAllSettings()
                },
                onTogglePlShade = {
                    isPlShade = !isPlShade
                    saveAllSettings()
                },
                onToggleMilkdropShade = {
                    isMilkdropShade = !isMilkdropShade
                    saveAllSettings()
                },
                isEqEnabled = isEqEnabled,
                preampDb = preampDb,
                bandsDb = bandsDb.value,
                visualizerDataProvider = { visualizerDataState.value },
                milkdropBridge = milkdropBridge,
                customPresets = customPresets,
                onPlay = playAction,
                onPause = pauseAction,
                onStop = stopAction,
                onPrev = prevAction,
                onNext = nextAction,
                onEject = {},
                onToggleEq = {
                    isEqShade = !isEqShade
                    saveAllSettings()
                },
                onTogglePl = {
                    isPlShade = !isPlShade
                    saveAllSettings()
                },
                onToggleMilkdrop = {
                    isMilkdropShade = !isMilkdropShade
                    saveAllSettings()
                },
                onToggleShuffle = {
                    isShuffle = !isShuffle
                    playbackService?.player?.shuffleModeEnabled = isShuffle
                    saveAllSettings()
                },
                onToggleRepeat = {
                    isRepeat = !isRepeat
                    playbackService?.player?.repeatMode = if (isRepeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                    saveAllSettings()
                },
                onToggleEqEnabled = {
                    isEqEnabled = !isEqEnabled
                    playbackService?.audioProcessor?.isEqEnabled = isEqEnabled
                    saveAllSettings()
                },
                onVolumeChange = { v ->
                    volume = v
                    playbackService?.player?.volume = v
                    prefs.saveAudioSettings(volume, balance, preampDb, bandsDb.value, isEqEnabled, isShuffle, isRepeat)
                },
                onBalanceChange = { b ->
                    balance = b
                    playbackService?.audioProcessor?.balance = b
                    prefs.saveAudioSettings(volume, balance, preampDb, bandsDb.value, isEqEnabled, isShuffle, isRepeat)
                },
                onSeek = { pct ->
                    playbackService?.player?.let { player ->
                        val duration = player.duration
                        if (duration > 0) {
                            player.seekTo((duration * pct).toLong())
                        }
                    }
                },
                onPreampChange = { db ->
                    preampDb = db
                    playbackService?.audioProcessor?.setPreampDb(db)
                    prefs.saveAudioSettings(volume, balance, preampDb, bandsDb.value, isEqEnabled, isShuffle, isRepeat)
                },
                onBandChange = { bandIdx, db ->
                    val newBands = bandsDb.value.copyOf()
                    newBands[bandIdx] = db
                    bandsDb.value = newBands
                    playbackService?.audioProcessor?.setBandGainDb(bandIdx, db)
                    prefs.saveAudioSettings(volume, balance, preampDb, bandsDb.value, isEqEnabled, isShuffle, isRepeat)
                },
                onSelectPreset = { preset ->
                    preampDb = preset.preampDb
                    bandsDb.value = preset.bands.copyOf()
                    playbackService?.audioProcessor?.applyPreset(preset)
                    prefs.saveAudioSettings(volume, balance, preampDb, bandsDb.value, isEqEnabled, isShuffle, isRepeat)
                },
                onSaveCustomPreset = { name ->
                    val p = com.winampdroid.core.audio.WinampEqualizerPreset(name, preampDb, bandsDb.value.copyOf())
                    prefs.saveCustomEqualizerPreset(p)
                    customPresets.clear()
                    customPresets.addAll(prefs.loadCustomEqualizerPresets())
                    Toast.makeText(this@MainActivity, "Preset '$name' guardado", Toast.LENGTH_SHORT).show()
                },
                onDeleteCustomPreset = { name ->
                    prefs.deleteCustomEqualizerPreset(name)
                    customPresets.clear()
                    customPresets.addAll(prefs.loadCustomEqualizerPresets())
                    Toast.makeText(this@MainActivity, "Preset '$name' eliminado", Toast.LENGTH_SHORT).show()
                },
                onTrackSelect = { idx ->
                    currentTrackIndex = idx
                    prefs.savePlaylist(tracks, currentTrackIndex)
                },
                onTrackPlay = { idx ->
                    isUserPaused = false
                    liveStreamMetadataTitle = null
                    currentTrackIndex = idx
                    playTrackAtIndex(tracks, idx)
                    prefs.savePlaylist(tracks, currentTrackIndex)
                },
                onAddTrack = {
                    audioPickerLauncher.launch(arrayOf("audio/*"))
                },
                onScanDeviceAudio = {
                    requestScanDeviceAudio()
                },
                onRemoveTrack = {
                    if (tracks.isNotEmpty() && currentTrackIndex in tracks.indices) {
                        tracks.removeAt(currentTrackIndex)
                        if (currentTrackIndex >= tracks.size) {
                            currentTrackIndex = maxOf(0, tracks.size - 1)
                        }
                        if (tracks.isEmpty()) {
                            isPlaying = false
                            isUserPaused = false
                            liveStreamMetadataTitle = null
                            timeElapsedSeconds = 0
                            totalDurationSeconds = 0
                            visualizerDataState.value = com.winampdroid.ui.desktop.VisualizerData()
                            playbackService?.clearPlaylist()
                        } else {
                            playbackService?.setPlaylist(tracks, currentTrackIndex, playNow = isPlaying)
                        }
                        prefs.savePlaylist(tracks, currentTrackIndex)
                    }
                },
                onClearTracks = {
                    tracks.clear()
                    currentTrackIndex = 0
                    isPlaying = false
                    isUserPaused = false
                    liveStreamMetadataTitle = null
                    timeElapsedSeconds = 0
                    totalDurationSeconds = 0
                    visualizerDataState.value = com.winampdroid.ui.desktop.VisualizerData()
                    playbackService?.clearPlaylist()
                    prefs.savePlaylist(tracks, 0)
                    Toast.makeText(this@MainActivity, "Lista de reproducción vaciada", Toast.LENGTH_SHORT).show()
                },
                onResetDefaultPlaylist = {
                    tracks.clear()
                    tracks.addAll(defaultTracksList)
                    currentTrackIndex = 0
                    playbackService?.setPlaylist(tracks, 0, playNow = false)
                    prefs.savePlaylist(tracks, 0)
                    Toast.makeText(this@MainActivity, "Lista por defecto restablecida (Intro + Radios)", Toast.LENGTH_SHORT).show()
                },
                onMoveTrack = { fromIdx, toIdx ->
                    if (fromIdx in tracks.indices && toIdx in tracks.indices) {
                        val item = tracks.removeAt(fromIdx)
                        tracks.add(toIdx, item)
                        if (currentTrackIndex == fromIdx) {
                            currentTrackIndex = toIdx
                        } else if (fromIdx < currentTrackIndex && toIdx >= currentTrackIndex) {
                            currentTrackIndex--
                        } else if (fromIdx > currentTrackIndex && toIdx <= currentTrackIndex) {
                            currentTrackIndex++
                        }
                        prefs.savePlaylist(tracks, currentTrackIndex)
                    }
                },
                onSortTracks = { mode ->
                    when (mode) {
                        "title" -> tracks.sortBy { it.title.lowercase() }
                        "artist" -> tracks.sortBy { it.artist.lowercase() }
                        "shuffle" -> tracks.shuffle()
                        "reverse" -> tracks.reverse()
                    }
                    prefs.savePlaylist(tracks, currentTrackIndex)
                    Toast.makeText(this@MainActivity, "Playlist reordenada ($mode)", Toast.LENGTH_SHORT).show()
                },
                onOpenOptions = {},
                builtInSkins = builtInSkins,
                downloadedSkins = downloadedSkins,
                activeSkinPath = prefs.getActiveSkin(),
                onSelectBuiltInSkin = { skinName, assetPath ->
                    val loaded = loadAssetSkin(assetPath)
                    if (loaded != null) {
                        skin = loaded
                        prefs.saveActiveSkin(assetPath)
                        Toast.makeText(this@MainActivity, "Skin '$skinName' aplicada", Toast.LENGTH_SHORT).show()
                    }
                },
                onSelectDownloadedSkin = { dlSkin ->
                    try {
                        val file = java.io.File(dlSkin.filePath)
                        if (file.exists()) {
                            file.inputStream().use { stream ->
                                val parsed = WszSkinParser.parse(stream, fallbackSkin = initialSkin)
                                skin = parsed
                                prefs.saveActiveSkin(dlSkin.filePath)
                                Toast.makeText(this@MainActivity, "Skin '${dlSkin.title}' aplicada", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "El archivo de la skin no existe", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Error al aplicar skin: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteDownloadedSkin = { dlSkin ->
                    try {
                        val file = java.io.File(dlSkin.filePath)
                        if (file.exists()) {
                            file.delete()
                        }
                        prefs.deleteDownloadedSkin(dlSkin.md5)
                        downloadedSkins.clear()
                        downloadedSkins.addAll(prefs.getDownloadedSkins())
                        if (prefs.getActiveSkin() == dlSkin.filePath) {
                            val base = loadAssetSkin("skins/base.wsz")
                            if (base != null) {
                                skin = base
                                prefs.saveActiveSkin("skins/base.wsz")
                            }
                        }
                        Toast.makeText(this@MainActivity, "Skin '${dlSkin.title}' eliminada", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onImportLocalSkin = {
                    skinPickerLauncher.launch("*/*")
                },
                onOpenSkinMuseum = {
                    showSkinMuseumDialog = true
                },
                onEnterPip = { enterPipMode() }
            )

            // Online Winamp Skin Museum Explorer Dialog
            if (showSkinMuseumDialog) {
                SkinMuseumDialog(
                    baseSkin = initialSkin,
                    onDismissRequest = { showSkinMuseumDialog = false },
                    onSkinApplied = { newSkin ->
                        skin = newSkin
                        downloadedSkins.clear()
                        downloadedSkins.addAll(prefs.getDownloadedSkins())
                    }
                )
            }
            } // Close else block of !isInPipMode
        }
    }

    private fun playTrackAtIndex(tracks: List<Track>, index: Int) {
        if (tracks.isEmpty() || index !in tracks.indices) {
            playbackService?.clearPlaylist()
            return
        }
        val targetTrack = tracks[index]
        playbackService?.let { service ->
            val player = service.player
            val currentMedia = if (index < player.mediaItemCount) player.getMediaItemAt(index) else null
            if (player.mediaItemCount == tracks.size && currentMedia?.mediaId == targetTrack.id) {
                player.seekToDefaultPosition(index)
                player.prepare()
                player.play()
            } else {
                service.setPlaylist(tracks, index, playNow = true)
            }
        }
    }

    private fun loadAssetSkin(path: String): WinampSkin? {
        return try {
            assets.open(path).use { WszSkinParser.parse(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractTrackMetadata(uri: Uri): Track {
        var title = ""
        var artist = "Artista Desconocido"
        var durationSecs = 0.0

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Artista Desconocido"
            val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toDoubleOrNull() ?: 0.0
            durationSecs = durMs / 1000.0
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        if (title.isBlank()) {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    title = cursor.getString(nameIndex).substringBeforeLast(".")
                }
            }
            if (title.isBlank()) {
                title = uri.lastPathSegment ?: "Canción"
            }
        }

        return Track(
            id = UUID.randomUUID().toString(),
            title = title,
            artist = artist,
            durationSeconds = durationSecs,
            uri = uri,
            isAsset = false
        )
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
