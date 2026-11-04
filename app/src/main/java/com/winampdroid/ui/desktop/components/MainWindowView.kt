package com.winampdroid.ui.desktop.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.winampdroid.core.skin.BitmapFontRenderer
import com.winampdroid.core.skin.WinampSkin
import kotlin.math.max

/**
 * Pixel-faithful, fully textured rendering of Winamp's MainWindow (275 x 116 px).
 * Matches the official Winamp 2.91 skin layout:
 * - Active TITLEBAR.BMP with bolt icon and window buttons
 * - Large digital LED counter from NUMBERS.BMP with Play/Pause state from PLAYPAUS.BMP
 * - Marquee display using TEXT.BMP font
 * - Bitrate (kbps) & Sample Rate (kHz) indicators and MONOSTER.BMP
 * - Interactive Volume and Balance sliders with grooved backgrounds (VOLUME.BMP, BALANCE.BMP)
 * - EQ and PL toggle buttons from SHUFREP.BMP with green active illumination
 * - POSBAR.BMP seek trackbar with golden handle
 * - Transport buttons from CBUTTONS.BMP
 * - SHUFFLE and REPEAT buttons with green active illumination from SHUFREP.BMP
 * - 60 FPS Visualizer toggle
 */
@Composable
fun MainWindowView(
    skin: WinampSkin?,
    trackTitle: String,
    timeElapsedSeconds: Int,
    totalDurationSeconds: Int,
    isPlaying: Boolean,
    isPaused: Boolean,
    volume: Float,
    balance: Float,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isEqOpen: Boolean,
    isPlOpen: Boolean,
    spectralData: FloatArray = FloatArray(0),
    pcmWaveData: ShortArray = ShortArray(0),
    visualizerDataProvider: (() -> com.winampdroid.ui.desktop.VisualizerData)? = null,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onEject: () -> Unit,
    onToggleEq: () -> Unit,
    onTogglePl: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onSeek: (Float) -> Unit,
    isShade: Boolean = false,
    onToggleShade: () -> Unit = {},
    onLoupeChange: (LoupeInfo) -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenMilkdropFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val mainBmp = skin?.getBitmap("MAIN")
    val titlebarBmp = skin?.getBitmap("TITLEBAR")
    val cbuttonsBmp = skin?.getBitmap("CBUTTONS")
    val shufrepBmp = skin?.getBitmap("SHUFREP") ?: skin?.getBitmap("SHUF")
    val textBmp = skin?.getBitmap("TEXT")
    val numbersBmp = skin?.getBitmap("NUMBERS")
    val playpausBmp = skin?.getBitmap("PLAYPAUS")
    val monosterBmp = skin?.getBitmap("MONOSTER")
    val volumeBmp = skin?.getBitmap("VOLUME")
    val balanceBmp = skin?.getBitmap("BALANCE")
    val posbarBmp = skin?.getBitmap("POSBAR")

    // Cache ImageBitmap wrappers to avoid allocations during 60 FPS drawing
    val mainImg = remember(mainBmp) { mainBmp?.asImageBitmap() }
    val titlebarImg = remember(titlebarBmp) { titlebarBmp?.asImageBitmap() }
    val cbuttonsImg = remember(cbuttonsBmp) { cbuttonsBmp?.asImageBitmap() }
    val shufrepImg = remember(shufrepBmp) { shufrepBmp?.asImageBitmap() }
    val numbersImg = remember(numbersBmp) { numbersBmp?.asImageBitmap() }
    val playpausImg = remember(playpausBmp) { playpausBmp?.asImageBitmap() }
    val monosterImg = remember(monosterBmp) { monosterBmp?.asImageBitmap() }
    val volumeImg = remember(volumeBmp) { volumeBmp?.asImageBitmap() }
    val balanceImg = remember(balanceBmp) { balanceBmp?.asImageBitmap() }
    val posbarImg = remember(posbarBmp) { posbarBmp?.asImageBitmap() }

    val fontRenderer = remember(textBmp) { BitmapFontRenderer(textBmp) }

    var visMode by remember { mutableStateOf(VisualizerMode.BARS) }
    var isTimeRemaining by remember { mutableStateOf(false) }
    var pressedButton by remember { mutableStateOf<String?>(null) }

    // Marquee scrolling offset
    var marqueeOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(trackTitle) {
        marqueeOffset = 0
        while (true) {
            kotlinx.coroutines.delay(120)
            marqueeOffset = (marqueeOffset + 1) % (maxOf(25, trackTitle.length + 5))
        }
    }

    var dragSeekPct by remember { mutableStateOf<Float?>(null) }
    val progressPercent = dragSeekPct ?: if (totalDurationSeconds > 0) {
        (timeElapsedSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    BoxWithConstraints(
        modifier = modifier.aspectRatio(if (isShade) (275f / 14f) else (275f / 116f))
    ) {
        val scale = maxWidth / 275.dp
        val winampScaleX = constraints.maxWidth.toFloat() / 275f
        val winampScaleY = constraints.maxHeight.toFloat() / (if (isShade) 14f else 116f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isShade) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val x = offset.x / winampScaleX
                            val y = offset.y / winampScaleY
                            if (y in 0f..14f) {
                                onToggleShade()
                            } else if (!isShade && x in 20f..105f && y in 18f..65f) {
                                onOpenMilkdropFullscreen()
                            }
                        },
                        onLongPress = { offset ->
                            val x = offset.x / winampScaleX
                            val y = offset.y / winampScaleY
                            val loupe = when {
                                x in 10f..39f && y in 80f..115f -> LoupeInfo(true, offset, "PREV", "◄◄ PREV", "Z")
                                x in 39f..62f && y in 80f..115f -> LoupeInfo(true, offset, "PLAY", "▶ PLAY", "X")
                                x in 62f..85f && y in 80f..115f -> LoupeInfo(true, offset, "PAUSE", "❚❚ PAUSE", "C")
                                x in 85f..108f && y in 80f..115f -> LoupeInfo(true, offset, "STOP", "■ STOP", "V")
                                x in 108f..135f && y in 80f..115f -> LoupeInfo(true, offset, "NEXT", "►► NEXT", "B")
                                x in 136f..160f && y in 85f..115f -> LoupeInfo(true, offset, "EJECT", "⏏ OPEN", "L")
                                x in 217f..241f && y in 56f..72f -> LoupeInfo(true, offset, "EQ", "🎛️ EQUALIZER", "Alt+G")
                                x in 242f..266f && y in 56f..72f -> LoupeInfo(true, offset, "PL", "📋 PLAYLIST", "Alt+E")
                                x in 162f..209f && y in 85f..110f -> LoupeInfo(true, offset, "SHUFFLE", "🔀 SHUFFLE", "S")
                                x in 210f..240f && y in 85f..110f -> LoupeInfo(true, offset, "REPEAT", "🔁 REPEAT", "R")
                                x in 241f..275f && y in 83f..115f -> LoupeInfo(true, offset, "ABOUT", "⚡ ACERCA DE WINAMP", "")
                                else -> null
                            }
                            loupe?.let { onLoupeChange(it) }
                        },
                        onPress = { offset ->
                            val x = offset.x / winampScaleX
                            val y = offset.y / winampScaleY
                            val btn = when {
                                x in 10f..39f && y in 80f..115f -> "PREV"
                                x in 39f..62f && y in 80f..115f -> "PLAY"
                                x in 62f..85f && y in 80f..115f -> "PAUSE"
                                x in 85f..108f && y in 80f..115f -> "STOP"
                                x in 108f..135f && y in 80f..115f -> "NEXT"
                                x in 136f..160f && y in 85f..115f -> "EJECT"
                                x in 241f..275f && y in 83f..115f -> "ABOUT"
                                else -> null
                            }
                            pressedButton = btn
                            tryAwaitRelease()
                            pressedButton = null
                            onLoupeChange(LoupeInfo(isVisible = false))
                        },
                        onTap = { offset ->
                            val x = offset.x / winampScaleX
                            val y = offset.y / winampScaleY

                            when {
                                // Close button: x [262..275], y [0..14] (Permanente, no cerrar)
                                x in 262f..275f && y in 0f..14f -> { /* No cerrar */ }
                                // Shade/Minimize button: x [252..262], y [0..14]
                                x in 252f..262f && y in 0f..14f -> onToggleShade()
                                // Options button: x [240..252], y [0..14]
                                x in 240f..252f && y in 0f..14f -> onEject()
                                // Previous button: x [16..39], y [88..106]
                                x in 10f..39f && y in 80f..115f -> onPrev()
                                // Play button: x [39..62], y [88..106]
                                x in 39f..62f && y in 80f..115f -> onPlay()
                                // Pause button: x [62..85], y [88..106]
                                x in 62f..85f && y in 80f..115f -> onPause()
                                // Stop button: x [85..108], y [88..106]
                                x in 85f..108f && y in 80f..115f -> onStop()
                                // Next button: x [108..130], y [88..106]
                                x in 108f..135f && y in 80f..115f -> onNext()
                                // Eject button: x [136..158], y [89..105]
                                x in 136f..160f && y in 85f..115f -> onEject()
                                // EQ toggle button: x [219..242], y [58..70]
                                x in 217f..241f && y in 56f..72f -> onToggleEq()
                                // PL toggle button: x [242..265], y [58..70]
                                x in 242f..266f && y in 56f..72f -> onTogglePl()
                                // Shuffle button: x [164..210], y [89..104]
                                x in 162f..209f && y in 85f..110f -> onToggleShuffle()
                                // Repeat button: x [210..238], y [89..104]
                                x in 210f..240f && y in 85f..110f -> onToggleRepeat()
                                // Winamp Lightning Bolt Logo (About Winamp): x [241..275], y [83..115]
                                x in 241f..275f && y in 83f..115f -> onOpenAbout()
                                // Posbar tap seek: x [16..264], y [68..84]
                                !isShade && x in 16f..264f && y in 68f..84f -> {
                                    val seekPct = ((x - 16f) / (248f - 29f)).coerceIn(0f, 1f)
                                    onSeek(seekPct)
                                }
                                // Volume tap: x [105..175], y [52..72]
                                !isShade && x in 105f..175f && y in 52f..72f -> {
                                    val v = ((x - 107f) / (68f - 14f)).coerceIn(0f, 1f)
                                    onVolumeChange(v)
                                }
                                // Balance tap: x [175..217], y [52..72]
                                !isShade && x in 175f..217f && y in 52f..72f -> {
                                    val b = (((x - 177f) / (38f - 14f)) * 2f - 1f).coerceIn(-1f, 1f)
                                    onBalanceChange(b)
                                }
                                // Time counter & music bars visor: tap to open Milkdrop fullscreen
                                !isShade && x in 20f..105f && y in 18f..65f -> {
                                    onOpenMilkdropFullscreen()
                                }
                            }
                        }
                    )
                }
            .pointerInput(isShade) {
                if (isShade) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        dragSeekPct?.let { onSeek(it) }
                        dragSeekPct = null
                    },
                    onDragCancel = {
                        dragSeekPct = null
                    }
                ) { change, _ ->
                    val x = change.position.x / winampScaleX
                    val y = change.position.y / winampScaleY

                    when {
                        // Posbar dragging
                        x in 10f..270f && y in 66f..86f -> {
                            val seekPct = ((x - 16f) / (248f - 29f)).coerceIn(0f, 1f)
                            dragSeekPct = seekPct
                        }
                        // Volume slider dragging
                        x in 100f..180f && y in 50f..74f -> {
                            val v = ((x - 107f) / (68f - 14f)).coerceIn(0f, 1f)
                            onVolumeChange(v)
                        }
                        // Balance slider dragging
                        x in 172f..220f && y in 50f..74f -> {
                            val b = (((x - 177f) / (38f - 14f)) * 2f - 1f).coerceIn(-1f, 1f)
                            onBalanceChange(b)
                        }
                    }
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 275f
                val scaleY = size.height / (if (isShade) 14f else 116f)

                // 0. Render Windowshade Mode (275 x 14 px)
                if (isShade) {
                    if (titlebarImg != null) {
                        val srcY = if (titlebarImg.height >= 43) 29 else 0
                        drawImage(
                            image = titlebarImg,
                            srcOffset = IntOffset(27, srcY),
                            srcSize = IntSize(275, 14),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                        // Options button
                        drawImage(
                            image = titlebarImg,
                            srcOffset = IntOffset(0, 0),
                            srcSize = IntSize(9, 9),
                            dstOffset = IntOffset((244 * scaleX).toInt(), (3 * scaleY).toInt()),
                            dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                        )
                        // Shade toggle button
                        drawImage(
                            image = titlebarImg,
                            srcOffset = IntOffset(9, 0),
                            srcSize = IntSize(9, 9),
                            dstOffset = IntOffset((254 * scaleX).toInt(), (3 * scaleY).toInt()),
                            dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                        )
                        // Close button
                        drawImage(
                            image = titlebarImg,
                            srcOffset = IntOffset(18, 0),
                            srcSize = IntSize(9, 9),
                            dstOffset = IntOffset((264 * scaleX).toInt(), (3 * scaleY).toInt()),
                            dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                        )
                    }
                    val timeElapsedStr = "%02d:%02d".format(timeElapsedSeconds / 60, timeElapsedSeconds % 60)
                    val shadeText = "${if (isPlaying) "▶" else if (isPaused) "❚❚" else "■"} $timeElapsedStr $trackTitle"
                    drawIntoCanvas { canvas ->
                        fontRenderer.drawText(
                            canvas = canvas.nativeCanvas,
                            text = shadeText,
                            startX = 28f * scaleX,
                            startY = 3f * scaleY,
                            scaleX = (scaleX * 0.85f).coerceAtLeast(0.7f),
                            scaleY = scaleY
                        )
                    }
                    return@Canvas
                }

                // 1. Draw Main Background (MAIN.BMP)
                if (mainImg != null) {
                    drawImage(
                        image = mainImg,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(275, 116),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                }

                // 2. Draw Titlebar (TITLEBAR.BMP)
                if (titlebarImg != null) {
                    // Titlebar main banner (active: x=27, y=0, w=275, h=14)
                    drawImage(
                        image = titlebarImg,
                        srcOffset = IntOffset(27, 0),
                        srcSize = IntSize(275, 14),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(size.width.toInt(), (14 * scaleY).toInt())
                    )
                    // Lightning bolt icon at (6, 3)
                    drawImage(
                        image = titlebarImg,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(9, 9),
                        dstOffset = IntOffset((6 * scaleX).toInt(), (3 * scaleY).toInt()),
                        dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                    )
                    // Options button at (244, 3)
                    drawImage(
                        image = titlebarImg,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(9, 9),
                        dstOffset = IntOffset((244 * scaleX).toInt(), (3 * scaleY).toInt()),
                        dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                    )
                    // Minimize button at (254, 3)
                    drawImage(
                        image = titlebarImg,
                        srcOffset = IntOffset(9, 0),
                        srcSize = IntSize(9, 9),
                        dstOffset = IntOffset((254 * scaleX).toInt(), (3 * scaleY).toInt()),
                        dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                    )
                    // Close button at (264, 3)
                    drawImage(
                        image = titlebarImg,
                        srcOffset = IntOffset(18, 0),
                        srcSize = IntSize(9, 9),
                        dstOffset = IntOffset((264 * scaleX).toInt(), (3 * scaleY).toInt()),
                        dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                    )
                }

                // 3. Draw Play/Pause Indicator from PLAYPAUS.BMP at (24, 28)
                if (playpausImg != null) {
                    val indicatorSrcX = when {
                        isPlaying && !isPaused -> 0  // Green Play Arrow ▶
                        isPaused -> 9                // Pause ||
                        else -> 18                   // Stop ■
                    }
                    drawImage(
                        image = playpausImg,
                        srcOffset = IntOffset(indicatorSrcX, 0),
                        srcSize = IntSize(9, 9),
                        dstOffset = IntOffset((24 * scaleX).toInt(), (28 * scaleY).toInt()),
                        dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                    )
                }

                // 4. Draw Digital Time LED Display (NUMBERS.BMP) at (48, 26)
                val displaySecs = if (isTimeRemaining) {
                    max(0, totalDurationSeconds - timeElapsedSeconds)
                } else {
                    timeElapsedSeconds
                }
                val mins = displaySecs / 60
                val secs = displaySecs % 60

                drawIntoCanvas { canvas ->
                    if (numbersBmp != null) {
                        fontRenderer.drawDigitalTime(
                            canvas = canvas.nativeCanvas,
                            numbersBitmap = numbersBmp,
                            minutes = mins,
                            seconds = secs,
                            startX = 48f * scaleX,
                            startY = 26f * scaleY,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )
                    }

                    // 5. Draw Marquee Text with TEXT.BMP at (112, 27)
                    val displayStr = " $trackTitle *** WINAMP *** "
                    val loopedStr = displayStr.drop(marqueeOffset) + displayStr.take(marqueeOffset)
                    fontRenderer.drawText(
                        canvas = canvas.nativeCanvas,
                        text = loopedStr.take(30),
                        startX = 112f * scaleX,
                        startY = 27f * scaleY,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )

                    // 6. Draw Bitrate & Sample Rate: "192 kbps", "44 kHz"
                    fontRenderer.drawText(
                        canvas = canvas.nativeCanvas,
                        text = "192",
                        startX = 112f * scaleX,
                        startY = 43f * scaleY,
                        scaleX = scaleX * 0.9f,
                        scaleY = scaleY * 0.9f
                    )
                    fontRenderer.drawText(
                        canvas = canvas.nativeCanvas,
                        text = "44",
                        startX = 156f * scaleX,
                        startY = 43f * scaleY,
                        scaleX = scaleX * 0.9f,
                        scaleY = scaleY * 0.9f
                    )
                }

                // 7. Draw Stereo / Mono from MONOSTER.BMP
                if (monosterImg != null) {
                    // Active stereo at (239, 41)
                    drawImage(
                        image = monosterImg,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(29, 12),
                        dstOffset = IntOffset((239 * scaleX).toInt(), (41 * scaleY).toInt()),
                        dstSize = IntSize((29 * scaleX).toInt(), (12 * scaleY).toInt())
                    )
                    // Dim mono at (212, 41)
                    drawImage(
                        image = monosterImg,
                        srcOffset = IntOffset(29, 12),
                        srcSize = IntSize(27, 12),
                        dstOffset = IntOffset((212 * scaleX).toInt(), (41 * scaleY).toInt()),
                        dstSize = IntSize((27 * scaleX).toInt(), (12 * scaleY).toInt())
                    )
                }

                // 8. Draw Volume Bar (VOLUME.BMP) at (107, 57)
                if (volumeImg != null) {
                    val volFrame = (volume * 27f).toInt().coerceIn(0, 27)
                    drawImage(
                        image = volumeImg,
                        srcOffset = IntOffset(0, volFrame * 15),
                        srcSize = IntSize(68, 13),
                        dstOffset = IntOffset((107 * scaleX).toInt(), (57 * scaleY).toInt()),
                        dstSize = IntSize((68 * scaleX).toInt(), (13 * scaleY).toInt())
                    )
                    val volThumbX = 107f + volume * (68f - 14f)
                    drawImage(
                        image = volumeImg,
                        srcOffset = IntOffset(15, 422),
                        srcSize = IntSize(14, 11),
                        dstOffset = IntOffset((volThumbX * scaleX).toInt(), (57 * scaleY).toInt()),
                        dstSize = IntSize((14 * scaleX).toInt(), (11 * scaleY).toInt())
                    )
                }

                // 9. Draw Balance Bar (BALANCE.BMP) at (177, 57)
                if (balanceImg != null) {
                    val balNormalized = ((balance + 1f) / 2f).coerceIn(0f, 1f)
                    val balFrame = (balNormalized * 27f).toInt().coerceIn(0, 27)
                    drawImage(
                        image = balanceImg,
                        srcOffset = IntOffset(9, balFrame * 15),
                        srcSize = IntSize(38, 13),
                        dstOffset = IntOffset((177 * scaleX).toInt(), (57 * scaleY).toInt()),
                        dstSize = IntSize((38 * scaleX).toInt(), (13 * scaleY).toInt())
                    )
                    val balThumbX = 177f + balNormalized * (38f - 14f)
                    drawImage(
                        image = balanceImg,
                        srcOffset = IntOffset(15, 422),
                        srcSize = IntSize(14, 11),
                        dstOffset = IntOffset((balThumbX * scaleX).toInt(), (57 * scaleY).toInt()),
                        dstSize = IntSize((14 * scaleX).toInt(), (11 * scaleY).toInt())
                    )
                }

                // 10. Draw EQ and PL Toggle Buttons (SHUFREP.BMP) at (219, 58) and (242, 58)
                if (shufrepImg != null) {
                    // EQ Button: normal (0, 61), active (0, 73)
                    val eqSrcY = if (isEqOpen) 73 else 61
                    drawImage(
                        image = shufrepImg,
                        srcOffset = IntOffset(0, eqSrcY),
                        srcSize = IntSize(23, 12),
                        dstOffset = IntOffset((219 * scaleX).toInt(), (58 * scaleY).toInt()),
                        dstSize = IntSize((23 * scaleX).toInt(), (12 * scaleY).toInt())
                    )
                    // PL Button: normal (23, 61), active (23, 73)
                    val plSrcY = if (isPlOpen) 73 else 61
                    drawImage(
                        image = shufrepImg,
                        srcOffset = IntOffset(23, plSrcY),
                        srcSize = IntSize(23, 12),
                        dstOffset = IntOffset((242 * scaleX).toInt(), (58 * scaleY).toInt()),
                        dstSize = IntSize((23 * scaleX).toInt(), (12 * scaleY).toInt())
                    )
                }

                // 11. Draw Position Slider (POSBAR.BMP) at (16, 72)
                if (posbarImg != null) {
                    drawImage(
                        image = posbarImg,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(248, 10),
                        dstOffset = IntOffset((16 * scaleX).toInt(), (72 * scaleY).toInt()),
                        dstSize = IntSize((248 * scaleX).toInt(), (10 * scaleY).toInt())
                    )
                    val thumbX = 16f + progressPercent * (248f - 29f)
                    drawImage(
                        image = posbarImg,
                        srcOffset = IntOffset(248, 0),
                        srcSize = IntSize(29, 10),
                        dstOffset = IntOffset((thumbX * scaleX).toInt(), (72 * scaleY).toInt()),
                        dstSize = IntSize((29 * scaleX).toInt(), (10 * scaleY).toInt())
                    )
                }

                // 12. Draw Transport Buttons (CBUTTONS.BMP) at y=88
                if (cbuttonsImg != null) {
                    val btnWidth = 23
                    val btnHeight = 18

                    val isPrevDepressed = pressedButton == "PREV"
                    val isPlayDepressed = pressedButton == "PLAY" || (isPlaying && !isPaused)
                    val isPauseDepressed = pressedButton == "PAUSE" || isPaused
                    val isStopDepressed = pressedButton == "STOP"
                    val isNextDepressed = pressedButton == "NEXT"
                    val isEjectDepressed = pressedButton == "EJECT"

                    // Previous |<<
                    drawImage(
                        image = cbuttonsImg,
                        srcOffset = IntOffset(0, if (isPrevDepressed) 18 else 0),
                        srcSize = IntSize(btnWidth, btnHeight),
                        dstOffset = IntOffset((16 * scaleX).toInt(), (88 * scaleY).toInt()),
                        dstSize = IntSize((btnWidth * scaleX).toInt(), (btnHeight * scaleY).toInt())
                    )
                    // Play ▶
                    drawImage(
                        image = cbuttonsImg,
                        srcOffset = IntOffset(23, if (isPlayDepressed) 18 else 0),
                        srcSize = IntSize(btnWidth, btnHeight),
                        dstOffset = IntOffset((39 * scaleX).toInt(), (88 * scaleY).toInt()),
                        dstSize = IntSize((btnWidth * scaleX).toInt(), (btnHeight * scaleY).toInt())
                    )
                    // Pause ||
                    drawImage(
                        image = cbuttonsImg,
                        srcOffset = IntOffset(46, if (isPauseDepressed) 18 else 0),
                        srcSize = IntSize(btnWidth, btnHeight),
                        dstOffset = IntOffset((62 * scaleX).toInt(), (88 * scaleY).toInt()),
                        dstSize = IntSize((btnWidth * scaleX).toInt(), (btnHeight * scaleY).toInt())
                    )
                    // Stop ■
                    drawImage(
                        image = cbuttonsImg,
                        srcOffset = IntOffset(69, if (isStopDepressed) 18 else 0),
                        srcSize = IntSize(btnWidth, btnHeight),
                        dstOffset = IntOffset((85 * scaleX).toInt(), (88 * scaleY).toInt()),
                        dstSize = IntSize((btnWidth * scaleX).toInt(), (btnHeight * scaleY).toInt())
                    )
                    // Next >>|
                    drawImage(
                        image = cbuttonsImg,
                        srcOffset = IntOffset(92, if (isNextDepressed) 18 else 0),
                        srcSize = IntSize(22, btnHeight),
                        dstOffset = IntOffset((108 * scaleX).toInt(), (88 * scaleY).toInt()),
                        dstSize = IntSize((22 * scaleX).toInt(), (btnHeight * scaleY).toInt())
                    )
                    // Eject ⏏
                    drawImage(
                        image = cbuttonsImg,
                        srcOffset = IntOffset(114, if (isEjectDepressed) 16 else 0),
                        srcSize = IntSize(22, 16),
                        dstOffset = IntOffset((136 * scaleX).toInt(), (89 * scaleY).toInt()),
                        dstSize = IntSize((22 * scaleX).toInt(), (16 * scaleY).toInt())
                    )
                }

                // 13. Draw Shuffle & Repeat Buttons (SHUFREP.BMP) at y=89
                if (shufrepImg != null) {
                    // Shuffle (47 x 15) at (164, 89): normal (28, 0), active (28, 30)
                    val shufSrcY = if (isShuffle) 30 else 0
                    drawImage(
                        image = shufrepImg,
                        srcOffset = IntOffset(28, shufSrcY),
                        srcSize = IntSize(47, 15),
                        dstOffset = IntOffset((164 * scaleX).toInt(), (89 * scaleY).toInt()),
                        dstSize = IntSize((47 * scaleX).toInt(), (15 * scaleY).toInt())
                    )
                    // Repeat (28 x 15) at (210, 89): normal (0, 0), active (0, 30)
                    val repSrcY = if (isRepeat) 30 else 0
                    drawImage(
                        image = shufrepImg,
                        srcOffset = IntOffset(0, repSrcY),
                        srcSize = IntSize(28, 15),
                        dstOffset = IntOffset((210 * scaleX).toInt(), (89 * scaleY).toInt()),
                        dstSize = IntSize((28 * scaleX).toInt(), (15 * scaleY).toInt())
                    )
                }
            }

            // 14. Embedded 75x16 pixel Visualizer spanning full LCD area under the timer
            Box(
                modifier = Modifier
                    .offset(x = 24.dp * scale, y = 43.dp * scale)
                    .size(75.dp * scale, 16.dp * scale)
            ) {
                if (visualizerDataProvider != null) {
                    IsolatedVisualizer(
                        mode = visMode,
                        dataProvider = visualizerDataProvider,
                        skinColors = skin?.colors ?: com.winampdroid.core.skin.SkinColors(),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SpectrumCanvas(
                        mode = visMode,
                        spectralData = spectralData,
                        pcmWaveData = pcmWaveData,
                        skinColors = skin?.colors ?: com.winampdroid.core.skin.SkinColors(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Isolated visualizer composable that subscribes to high-frequency audio data updates.
 * Prevents MainWindowView from recomposing on every audio frame.
 */
@Composable
private fun IsolatedVisualizer(
    mode: VisualizerMode,
    dataProvider: () -> com.winampdroid.ui.desktop.VisualizerData,
    skinColors: com.winampdroid.core.skin.SkinColors,
    modifier: Modifier = Modifier
) {
    val data = dataProvider()
    SpectrumCanvas(
        mode = mode,
        spectralData = data.spectralData,
        pcmWaveData = data.pcmWaveData,
        skinColors = skinColors,
        modifier = modifier
    )
}
