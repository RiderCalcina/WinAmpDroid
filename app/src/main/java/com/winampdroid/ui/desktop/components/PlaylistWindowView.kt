package com.winampdroid.ui.desktop.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winampdroid.core.model.Track
import com.winampdroid.core.skin.WinampSkin
import kotlinx.coroutines.launch

/**
 * Pixel-faithful, fully skinned rendering of Winamp's Playlist Window (275 x variable height).
 * Uses PLEDIT.BMP from the active Winamp skin:
 * - Authentic top header titlebar (WINAMP PLAYLIST with close and minimize)
 * - Quick Search / "Jump to File" bar (instant real-time search across hundreds of tracks)
 * - Skinned left and right borders
 * - Classic retro track rows (yellow numbering and text with blue active selection)
 * - Move Up / Move Down buttons on selected track
 * - Authentic 38px bottom bar with metallic ADD, REM, SEL, MISC, time and LIST OPTS buttons
 * - Advanced Playlist Management modal (sort, randomize, reverse, reorder, search)
 */
@Composable
fun PlaylistWindowView(
    skin: WinampSkin?,
    tracks: List<Track>,
    currentTrackIndex: Int,
    isPlaying: Boolean,
    onTrackSelect: (Int) -> Unit,
    onTrackPlay: (Int) -> Unit = {},
    onAddTrack: () -> Unit,
    onScanDeviceAudio: () -> Unit,
    onRemoveTrack: () -> Unit,
    onClearTracks: () -> Unit,
    onClose: () -> Unit,
    onMoveTrack: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onSortTracks: (mode: String) -> Unit = {},
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onEject: () -> Unit = {},
    onOpenOptions: () -> Unit = {},
    isSearchActive: Boolean = false,
    onToggleSearch: () -> Unit = {},
    onCloseSearch: () -> Unit = onToggleSearch,
    onOpenManager: (RetroMenuTab) -> Unit = {},
    isShade: Boolean = false,
    onToggleShade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pleditBmp = skin?.getBitmap("PLEDIT")
    val pleditImg = remember(pleditBmp) { pleditBmp?.asImageBitmap() }

    var searchQuery by remember { mutableStateOf("") }

    val totalSecs = remember(tracks.size, tracks.firstOrNull()) { tracks.sumOf { it.durationSeconds }.toInt() }
    val totalTimeStr = remember(totalSecs) { "%d:%02d".format(totalSecs / 60, totalSecs % 60) }
    val curTrackSecs = tracks.getOrNull(currentTrackIndex)?.durationSeconds?.toInt() ?: 0
    val curTimeStr = "%d:%02d".format(curTrackSecs / 60, curTrackSecs % 60)

    val activeBgColor = remember(skin) { skin?.colors?.let { Color(it.plSelectedBg) } ?: Color(0xFF000088) }
    val normalTextColor = remember(skin) { skin?.colors?.let { Color(it.plNormalText) } ?: Color(0xFF00FF00) }
    val activeTextColor = remember(skin) { skin?.colors?.let { Color(it.plCurrentText) } ?: Color(0xFFFFFF00) }
    val normalBgColor = remember(skin) { skin?.colors?.let { Color(it.plNormalBg) } ?: Color.Black }

    // Pair of (masterIndex, track) - Optimizado con derivedStateOf para evitar filtrados repetitivos en cada frame
    val displayedTracks by remember(tracks, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                tracks.mapIndexed { index, track -> Pair(index, track) }
            } else {
                tracks.mapIndexed { index, track -> Pair(index, track) }
                    .filter { (_, track) ->
                        track.title.contains(searchQuery, ignoreCase = true) ||
                        track.artist.contains(searchQuery, ignoreCase = true)
                    }
            }
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to currently playing track whenever it changes (Garantiza visibilidad en modo Aleatorio/Shuffle)
    LaunchedEffect(currentTrackIndex, displayedTracks.size) {
        val targetPos = displayedTracks.indexOfFirst { it.first == currentTrackIndex }
        if (targetPos >= 0) {
            val visibleIndices = listState.layoutInfo.visibleItemsInfo.map { it.index }
            if (targetPos !in visibleIndices) {
                val scrollTarget = (targetPos - 2).coerceAtLeast(0)
                listState.animateScrollToItem(scrollTarget)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val widthDp = maxWidth
        val scale = widthDp / 275.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isShade) Modifier.height(20.dp * scale)
                    else Modifier.fillMaxHeight()
                )
                .background(Color.Black)
        ) {
            // 1. Skinned Top Header Bar (20px high in Winamp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp * scale)
                    .background(Color.Black)
                    .pointerInput(isShade) {
                        detectTapGestures(
                            onDoubleTap = { onToggleShade() }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 275f

                    if (pleditImg != null) {
                        val leftW = (25 * scaleX).toInt()
                        val rightW = (25 * scaleX).toInt()
                        val fillW = (size.width - leftW - rightW).toInt()

                        // Left header corner: (x=0, y=0, w=25, h=20)
                        drawImage(
                            image = pleditImg,
                            srcOffset = IntOffset(0, 0),
                            srcSize = IntSize(25, 20),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(leftW, size.height.toInt())
                        )

                        // Center header fill: (x=128, y=0, w=23, h=20) - Relleno continuo limpio sin líneas divisorias de plantilla
                        if (fillW > 0) {
                            drawImage(
                                image = pleditImg,
                                srcOffset = IntOffset(128, 0),
                                srcSize = IntSize(23, 20),
                                dstOffset = IntOffset(leftW, 0),
                                dstSize = IntSize(fillW, size.height.toInt())
                            )
                        }

                        // Right header corner: (x=153, y=0, w=25, h=20)
                        drawImage(
                            image = pleditImg,
                            srcOffset = IntOffset(153, 0),
                            srcSize = IntSize(25, 20),
                            dstOffset = IntOffset((size.width - rightW).toInt(), 0),
                            dstSize = IntSize(rightW, size.height.toInt())
                        )
                    }
                }
            }

            // Quick Search / Jump to File Input Bar
            if (isSearchActive && !isShade) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp * scale)
                        .background(Color(0xFF001A00))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUSCAR: ",
                        color = Color(0xFF00FF66),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF00FF66),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            color = Color(0xFFFF5555),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(horizontal = 6.dp)
                        )
                    }
                    Text(
                        text = "CERRAR",
                        color = Color(0xFF888899),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable {
                                onCloseSearch()
                                searchQuery = ""
                            }
                            .padding(start = 4.dp)
                    )
                }
            }

            if (!isShade) {
                // 2. Middle Section: Skinned Borders + Track List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Left border (12px wide in Winamp)
                    Box(
                        modifier = Modifier
                            .width(12.dp * scale)
                            .fillMaxHeight()
                            .background(Color.Black)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (pleditImg != null) {
                                var curY = 0
                                val tileH = (29 * (size.width / 12f)).toInt().coerceAtLeast(1)
                                while (curY < size.height.toInt()) {
                                    val h = minOf(tileH, size.height.toInt() - curY)
                                    drawImage(
                                        image = pleditImg,
                                        srcOffset = IntOffset(0, 42),
                                        srcSize = IntSize(11, 29),
                                        dstOffset = IntOffset(0, curY),
                                        dstSize = IntSize(size.width.toInt(), h)
                                    )
                                    curY += h
                                }
                            }
                        }
                    }

                    // Scrollable Track List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(normalBgColor)
                            .padding(horizontal = 2.dp)
                    ) {
                        items(displayedTracks, key = { "${it.first}_${it.second.id}" }) { item ->
                            val masterIndex = item.first
                            val track = item.second
                            val isSelected = masterIndex == currentTrackIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) activeBgColor else Color.Transparent)
                                    .pointerInput(masterIndex) {
                                        detectTapGestures(
                                            onTap = { onTrackSelect(masterIndex) },
                                            onDoubleTap = {
                                                onTrackSelect(masterIndex)
                                                onTrackPlay(masterIndex)
                                            }
                                        )
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${masterIndex + 1}. ${track.artist} - ${track.title}",
                                    color = if (isSelected) activeTextColor else normalTextColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )

                                // Reordering buttons when row is selected
                                if (isSelected && tracks.size > 1 && searchQuery.isEmpty()) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (masterIndex > 0) {
                                            Text(
                                                text = "▲",
                                                color = activeTextColor,
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .background(Color(0x33FFFFFF))
                                                    .clickable { onMoveTrack(masterIndex, masterIndex - 1) }
                                                    .padding(horizontal = 3.dp)
                                            )
                                        }
                                        if (masterIndex < tracks.size - 1) {
                                            Text(
                                                text = "▼",
                                                color = activeTextColor,
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .background(Color(0x33FFFFFF))
                                                    .clickable { onMoveTrack(masterIndex, masterIndex + 1) }
                                                    .padding(horizontal = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = track.formatDuration(),
                                    color = if (isSelected) activeTextColor else normalTextColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    // Right border with Isolated Interactive Retro Scrollbar (20px wide in Winamp)
                    RetroPlaylistScrollbar(
                        listState = listState,
                        totalCount = displayedTracks.size,
                        pleditImg = pleditImg,
                        modifier = Modifier
                            .width(20.dp * scale)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                }

                // 3. Skinned Bottom Action Bar (38px high in Winamp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp * scale)
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val scaleX = size.width / 275f
                                val scaleY = size.height / 38f
                                val x = offset.x / scaleX
                                val y = offset.y / scaleY

                                when {
                                    // ADD button: x [10..38], y [8..32]
                                    x in 10f..38f && y in 8f..32f -> onAddTrack()
                                    // REM button: x [39..67], y [8..32]
                                    x in 39f..67f && y in 8f..32f -> onRemoveTrack()
                                    // SEL button: x [68..96], y [8..32] -> Toggle Search or Scan
                                    x in 68f..96f && y in 8f..32f -> onToggleSearch()
                                    // MISC button: x [97..124], y [8..32] -> Open Retro Playlist Manager
                                    x in 97f..124f && y in 8f..32f -> onOpenManager(RetroMenuTab.PLAYLIST)
                                    // Mini Previous |<<: x [125..136], y [19..36]
                                    x in 125f..136f && y in 19f..36f -> onPrev()
                                    // Mini Play ▶: x [137..147], y [19..36]
                                    x in 137f..147f && y in 19f..36f -> onPlay()
                                    // Mini Pause ||: x [148..158], y [19..36]
                                    x in 148f..158f && y in 19f..36f -> onPause()
                                    // Mini Stop ■: x [159..169], y [19..36]
                                    x in 159f..169f && y in 19f..36f -> onStop()
                                    // Mini Next >>|: x [170..180], y [19..36]
                                    x in 170f..180f && y in 19f..36f -> onNext()
                                    // Mini Eject ⏏: x [181..195], y [19..36]
                                    x in 181f..195f && y in 19f..36f -> onEject()
                                    // LIST OPTS button: x [225..255], y [8..32] -> Open Retro Skins & Options
                                    x in 225f..255f && y in 8f..32f -> onOpenManager(RetroMenuTab.SKINS)
                                    // Time display click:
                                    x in 126f..220f && y in 6f..19f -> onScanDeviceAudio()
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scaleX = size.width / 275f

                        if (pleditImg != null) {
                            val leftW = (125 * scaleX).toInt()
                            val rightW = size.width.toInt() - leftW

                            // Bottom left corner (124 x 38) with ADD and REM
                            drawImage(
                                image = pleditImg,
                                srcOffset = IntOffset(0, 72),
                                srcSize = IntSize(124, 38),
                                dstOffset = IntOffset(0, 0),
                                dstSize = IntSize(leftW, size.height.toInt())
                            )
                            // Bottom right corner (149 x 38) with SEL, MISC, TIME and LIST OPTS
                            drawImage(
                                image = pleditImg,
                                srcOffset = IntOffset(126, 72),
                                srcSize = IntSize(149, 38),
                                dstOffset = IntOffset(leftW, 0),
                                dstSize = IntSize(rightW, size.height.toInt())
                            )
                        }
                    }

                    // Authentic Winamp Digital Running Time Display (x=132, y=9)
                    Box(
                        modifier = Modifier
                            .offset(x = 132.dp * scale, y = 7.dp * scale)
                    ) {
                        Text(
                            text = "$curTimeStr / $totalTimeStr",
                            color = Color(0xFF00FF00),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Isolated Retro Scrollbar component.
 * Performs all scroll position calculation and drawing strictly inside Canvas DrawScope,
 * completely preventing recomposition of PlaylistWindowView during touch and scroll gestures.
 */
@Composable
private fun RetroPlaylistScrollbar(
    listState: LazyListState,
    totalCount: Int,
    pleditImg: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .pointerInput(totalCount) {
                detectVerticalDragGestures { change, _ ->
                    if (totalCount > 1) {
                        val fraction = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val targetIndex = (fraction * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                        coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
            .pointerInput(totalCount) {
                detectTapGestures { offset ->
                    if (totalCount > 1) {
                        val fraction = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val targetIndex = (fraction * (totalCount - 1)).toInt().coerceIn(0, totalCount - 1)
                        coroutineScope.launch {
                            listState.scrollToItem(targetIndex)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (pleditImg != null) {
                val scaleFactor = size.width / 20f
                var curY = 0
                val tileH = (29 * scaleFactor).toInt().coerceAtLeast(1)
                while (curY < size.height.toInt()) {
                    val h = minOf(tileH, size.height.toInt() - curY)
                    drawImage(
                        image = pleditImg,
                        srcOffset = IntOffset(31, 42),
                        srcSize = IntSize(19, 29),
                        dstOffset = IntOffset(0, curY),
                        dstSize = IntSize(size.width.toInt(), h)
                    )
                    curY += h
                }

                // Dynamic scroll position calculation strictly inside DrawScope (0 recompositions!)
                val layoutInfo = listState.layoutInfo
                val visibleCount = layoutInfo.visibleItemsInfo.size
                val maxScroll = (totalCount - visibleCount).coerceAtLeast(1)
                val scrollProgress = if (totalCount <= visibleCount || maxScroll == 0) {
                    0f
                } else {
                    val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
                    val itemFraction = if (itemSize > 0) listState.firstVisibleItemScrollOffset.toFloat() / itemSize else 0f
                    ((listState.firstVisibleItemIndex + itemFraction) / maxScroll.toFloat()).coerceIn(0f, 1f)
                }

                val thumbW = (8 * scaleFactor).toInt().coerceAtLeast(1)
                val thumbH = (18 * scaleFactor).toInt().coerceIn(18, size.height.toInt())
                val maxTravel = (size.height.toInt() - thumbH).coerceAtLeast(0)
                val thumbY = (scrollProgress * maxTravel).toInt()
                val thumbX = (6 * scaleFactor).toInt()

                drawImage(
                    image = pleditImg,
                    srcOffset = IntOffset(53, 53),
                    srcSize = IntSize(8, 18),
                    dstOffset = IntOffset(thumbX, thumbY),
                    dstSize = IntSize(thumbW, thumbH)
                )
            }
        }
    }
}
