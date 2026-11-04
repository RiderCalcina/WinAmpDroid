package com.winampdroid.ui.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winampdroid.core.audio.WinampEqualizerPreset
import com.winampdroid.core.model.Track
import com.winampdroid.core.skin.WinampSkin
import com.winampdroid.core.storage.DownloadedSkin
import com.winampdroid.core.visualizer.NullsoftFFT
import com.winampdroid.ui.desktop.components.EqualizerWindowView
import com.winampdroid.ui.desktop.components.MainWindowView
import com.winampdroid.ui.desktop.components.MilkdropWindowView
import com.winampdroid.ui.desktop.components.PlaylistWindowView
import com.winampdroid.ui.desktop.components.LoupeInfo
import com.winampdroid.ui.desktop.components.WinampTouchLoupeLayer
import com.winampdroid.ui.desktop.components.RetroControlCenterDialog
import com.winampdroid.ui.desktop.components.RetroMenuTab
import com.winampdroid.ui.desktop.components.RetroAboutDialog
import com.winampdroid.ui.desktop.components.RetroEqualizerPresetDialog

/**
 * Modo Retro Desktop (Adaptive for Mobile Phones).
 *
 * Automatically calculates scale factor based on mobile screen width:
 * scale = screenWidth / 275dp
 *
 * Supports:
 * - Smart Vertical Docking (Main + EQ + Playlist + Milkdrop stacked neatly with 0dp gap).
 * - Windowshade mode (collapses any window to a 14px or 20px bar).
 * - Touch Loupe Layer (floating magnifying lens for micro buttons).
 * - 60 FPS Nullsoft FFT spectrum visualizer.
 * - Interactive seeking trackbar (POSBAR) & Volume/Balance sliders.
 * - 13 Authentic Factory Presets + Custom Equalizer Presets management.
 * - Advanced Playlist Management (reorder up/down, sort by title/artist, shuffle, reverse).
 * - Immersive Milkdrop Fullscreen mode.
 */
@Composable
fun RetroDesktopLayout(
    skin: WinampSkin?,
    tracks: List<Track>,
    currentTrackIndex: Int,
    isPlaying: Boolean,
    isPaused: Boolean,
    timeElapsedSeconds: Int,
    totalDurationSeconds: Int,
    volume: Float,
    balance: Float,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isEqOpen: Boolean,
    isPlOpen: Boolean,
    isMilkdropOpen: Boolean = true,
    isMainShade: Boolean = false,
    isEqShade: Boolean = false,
    isPlShade: Boolean = false,
    isMilkdropShade: Boolean = false,
    onToggleMainShade: () -> Unit = {},
    onToggleEqShade: () -> Unit = {},
    onTogglePlShade: () -> Unit = {},
    onToggleMilkdropShade: () -> Unit = {},
    isEqEnabled: Boolean,
    preampDb: Float,
    bandsDb: FloatArray,
    pcmWaveData: ShortArray = ShortArray(0),
    spectralData: FloatArray? = null,
    visualizerDataProvider: (() -> VisualizerData)? = null,
    milkdropBridge: com.winampdroid.ui.desktop.components.MilkdropBridge? = null,
    customPresets: List<WinampEqualizerPreset> = emptyList(),
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onEject: () -> Unit,
    onToggleEq: () -> Unit,
    onTogglePl: () -> Unit,
    onToggleMilkdrop: () -> Unit = {},
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleEqEnabled: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onSeek: (Float) -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onSelectPreset: (WinampEqualizerPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit = {},
    onDeleteCustomPreset: (String) -> Unit = {},
    onTrackSelect: (Int) -> Unit,
    onTrackPlay: (Int) -> Unit = {},
    onAddTrack: () -> Unit,
    onScanDeviceAudio: () -> Unit,
    onRemoveTrack: () -> Unit,
    onClearTracks: () -> Unit,
    onResetDefaultPlaylist: () -> Unit = {},
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onSortTracks: (mode: String) -> Unit = {},
    onOpenOptions: () -> Unit = {},
    builtInSkins: List<Pair<String, String>> = emptyList(),
    downloadedSkins: List<DownloadedSkin> = emptyList(),
    activeSkinPath: String? = null,
    onSelectBuiltInSkin: (String, String) -> Unit = { _, _ -> },
    onSelectDownloadedSkin: (DownloadedSkin) -> Unit = {},
    onDeleteDownloadedSkin: (DownloadedSkin) -> Unit = {},
    onImportLocalSkin: () -> Unit = {},
    onOpenSkinMuseum: () -> Unit = {},
    onEnterPip: (() -> Unit)? = null,
    displayTitleOverride: String? = null,
    modifier: Modifier = Modifier
) {
    val currentTrack = tracks.getOrNull(currentTrackIndex)
    val trackTitle = displayTitleOverride ?: currentTrack?.let { "${it.artist} - ${it.title}" } ?: "WINAMP"

    // FFT Nullsoft Engine (converts PCM to frequency bands if not provided)
    val internalFft = remember { NullsoftFFT() }
    val internalSpectralData = remember { FloatArray(512) { 0f } }
    val floatPcmBuffer = remember { FloatArray(1024) { 0f } }

    val activeSpectralData = spectralData ?: internalSpectralData

    LaunchedEffect(pcmWaveData) {
        if (visualizerDataProvider == null && spectralData == null && isPlaying && !isPaused && pcmWaveData.isNotEmpty()) {
            for (i in floatPcmBuffer.indices) {
                floatPcmBuffer[i] = if (i < pcmWaveData.size) pcmWaveData[i] / 32768f else 0f
            }
            internalFft.timeToFrequencyDomain(floatPcmBuffer, internalSpectralData)
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }
    var showControlCenterDialog by remember { mutableStateOf(false) }
    var controlCenterInitialTab by remember { mutableStateOf(RetroMenuTab.PLAYLIST) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showFullscreenMilkdrop by remember { mutableStateOf(false) }

    var showPresetDialog by remember { mutableStateOf(false) }
    var loupeInfo by remember { mutableStateOf(LoupeInfo()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 1. MainWindow (Auto-fitted to phone screen width, 275px classic coordinate space)
            MainWindowView(
                skin = skin,
                trackTitle = trackTitle,
                timeElapsedSeconds = timeElapsedSeconds,
                totalDurationSeconds = totalDurationSeconds,
                isPlaying = isPlaying,
                isPaused = isPaused,
                volume = volume,
                balance = balance,
                isShuffle = isShuffle,
                isRepeat = isRepeat,
                isEqOpen = isEqOpen,
                isPlOpen = isPlOpen,
                spectralData = activeSpectralData,
                pcmWaveData = pcmWaveData,
                visualizerDataProvider = visualizerDataProvider,
                onPlay = onPlay,
                onPause = onPause,
                onStop = onStop,
                onPrev = onPrev,
                onNext = onNext,
                onEject = {
                    controlCenterInitialTab = RetroMenuTab.PLAYLIST
                    showControlCenterDialog = true
                },
                onToggleEq = onToggleEq,
                onTogglePl = onTogglePl,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onVolumeChange = onVolumeChange,
                onBalanceChange = onBalanceChange,
                onSeek = onSeek,
                isShade = false,
                onToggleShade = {},
                onLoupeChange = { loupeInfo = it },
                onOpenAbout = { showAboutDialog = true },
                onOpenMilkdropFullscreen = { showFullscreenMilkdrop = true },
                modifier = Modifier.fillMaxWidth()
            )

            // 2. EqualizerWindow (Docked below Main - Permanent)
            EqualizerWindowView(
                skin = skin,
                isEqEnabled = isEqEnabled,
                preampDb = preampDb,
                bandsDb = bandsDb,
                onToggleEq = onToggleEqEnabled,
                onPreampChange = onPreampChange,
                onBandChange = onBandChange,
                onPresetClick = { showPresetDialog = true },
                onClose = {},
                isShade = false,
                onToggleShade = {},
                modifier = Modifier.fillMaxWidth()
            )

            // 3. PlaylistWindow (Docked below EQ - Se extiende verticalmente ocupando el lugar de Milkdrop)
            PlaylistWindowView(
                skin = skin,
                tracks = tracks,
                currentTrackIndex = currentTrackIndex,
                isPlaying = isPlaying,
                onTrackSelect = onTrackSelect,
                onTrackPlay = onTrackPlay,
                onAddTrack = onAddTrack,
                onScanDeviceAudio = onScanDeviceAudio,
                onRemoveTrack = onRemoveTrack,
                onClearTracks = onClearTracks,
                onClose = {},
                onMoveTrack = onMoveTrack,
                onSortTracks = onSortTracks,
                onPlay = onPlay,
                onPause = onPause,
                onStop = onStop,
                onPrev = onPrev,
                onNext = onNext,
                onEject = {
                    controlCenterInitialTab = RetroMenuTab.PLAYLIST
                    showControlCenterDialog = true
                },
                onOpenOptions = {
                    controlCenterInitialTab = RetroMenuTab.SKINS
                    showControlCenterDialog = true
                },
                isSearchActive = isSearchActive,
                onToggleSearch = { isSearchActive = !isSearchActive },
                onOpenManager = { tab ->
                    controlCenterInitialTab = tab
                    showControlCenterDialog = true
                },
                isShade = false,
                onToggleShade = {},
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        // Floating Retro Touch Loupe Layer (Magnifying glass for mouse-sized micro-buttons)
        WinampTouchLoupeLayer(loupeInfo = loupeInfo)

        // Equalizer Presets Selection & Management Dialog (Retro Cyberpunk Style)
        if (showPresetDialog) {
            RetroEqualizerPresetDialog(
                customPresets = customPresets,
                onSelectPreset = { preset ->
                    onSelectPreset(preset)
                    showPresetDialog = false
                },
                onSaveCustomPreset = { name ->
                    onSaveCustomPreset(name)
                },
                onDeleteCustomPreset = onDeleteCustomPreset,
                onDismissRequest = { showPresetDialog = false }
            )
        }

        // 5. Unified Retro Control Center (Administrador de Playlist, Selector de Skins Locales, Skin Museum)
        RetroControlCenterDialog(
            isOpen = showControlCenterDialog,
            initialTab = controlCenterInitialTab,
            onDismissRequest = { showControlCenterDialog = false },
            isSearchActive = isSearchActive,
            onToggleSearch = { isSearchActive = !isSearchActive },
            onSortTracks = onSortTracks,
            onScanDeviceAudio = onScanDeviceAudio,
            onAddAudioFiles = onAddTrack,
            onClearTracks = onClearTracks,
            onResetDefaultPlaylist = onResetDefaultPlaylist,
            builtInSkins = builtInSkins,
            downloadedSkins = downloadedSkins,
            activeSkinPath = activeSkinPath,
            onSelectBuiltInSkin = onSelectBuiltInSkin,
            onSelectDownloadedSkin = onSelectDownloadedSkin,
            onDeleteDownloadedSkin = onDeleteDownloadedSkin,
            onImportLocalSkin = onImportLocalSkin,
            onOpenSkinMuseum = onOpenSkinMuseum
        )

        // 6. Retro About Dialog (De un fan para fans de Winamp - www.qwertyaserty.com)
        if (showAboutDialog) {
            RetroAboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }

        // 7. Fullscreen Milkdrop Visualizer Dialog (activado por tap/doble tap en visor de tiempo / barras de MainWindow)
        if (showFullscreenMilkdrop) {
            val defaultBridge = remember { com.winampdroid.ui.desktop.components.MilkdropBridge() }
            com.winampdroid.ui.desktop.components.MilkdropFullscreenDialog(
                trackTitle = trackTitle,
                isPlaying = isPlaying,
                isPaused = isPaused,
                milkdropBridge = milkdropBridge ?: defaultBridge,
                onDismissRequest = { showFullscreenMilkdrop = false }
            )
        }
    }
}
