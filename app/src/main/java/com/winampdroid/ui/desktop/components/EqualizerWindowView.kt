package com.winampdroid.ui.desktop.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.winampdroid.core.skin.WinampSkin

/**
 * Pixel-faithful, fully textured rendering of Winamp's Equalizer Window (275 x 116 px).
 * Matches the official Winamp 2.91 skin layout:
 * - Active WINAMP EQUALIZER Titlebar from EQMAIN.BMP with close button
 * - ON button with green active illumination from EQMAIN.BMP
 * - AUTO button from EQMAIN.BMP
 * - Equalizer Spline Curve Graph in real-time
 * - PRESETS button from EQMAIN.BMP
 * - 10 Band sliders + Preamp with yellow groove tracks and metallic handles from EQMAIN.BMP
 */
@Composable
fun EqualizerWindowView(
    skin: WinampSkin?,
    isEqEnabled: Boolean,
    preampDb: Float, // -12dB to +12dB
    bandsDb: FloatArray, // 10 bands
    onToggleEq: () -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onPresetClick: () -> Unit,
    onClose: () -> Unit,
    isShade: Boolean = false,
    onToggleShade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val eqMainBmp = skin?.getBitmap("EQMAIN")
    val eqMainImg = remember(eqMainBmp) { eqMainBmp?.asImageBitmap() }

    // Band X positions in classic Winamp
    val sliderXPositions = remember {
        floatArrayOf(21f, 78f, 96f, 114f, 132f, 150f, 168f, 186f, 204f, 222f, 240f)
    }

    val sliderTopY = 38f
    val sliderHeight = 63f

    val shadeEqPath = remember { Path() }
    val normalEqPath = remember { Path() }

    BoxWithConstraints(
        modifier = modifier.aspectRatio(if (isShade) (275f / 14f) else (275f / 116f))
    ) {
        val winampScaleX = constraints.maxWidth.toFloat() / 275f
        val winampScaleY = constraints.maxHeight.toFloat() / (if (isShade) 14f else 116f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isShade) {
                    detectTapGestures(
                        onDoubleTap = { /* Permanente, no minimizar */ },
                        onTap = { offset ->
                            val x = offset.x / winampScaleX
                            val y = offset.y / winampScaleY

                            when {
                                // Close button: x [264..273], y [3..12] (Permanente, no cerrar)
                                x in 260f..275f && y in 0f..14f -> { /* Permanente, no cerrar */ }
                                // Shade button: x [250..260], y [0..14] (Permanente, no minimizar)
                                x in 250f..260f && y in 0f..14f -> { /* Permanente, no minimizar */ }
                                // ON button: x [14..39], y [18..30]
                                !isShade && x in 12f..39f && y in 16f..32f -> onToggleEq()
                                // Presets button: x [217..261], y [18..30]
                                !isShade && x in 215f..265f && y in 16f..32f -> onPresetClick()
                                else -> {
                                    if (!isShade) {
                                        // Direct tap on sliders
                                        for (i in sliderXPositions.indices) {
                                            val sx = sliderXPositions[i]
                                            if (x in (sx - 9f)..(sx + 20f) && y in (sliderTopY - 10f)..(sliderTopY + sliderHeight + 10f)) {
                                                val normalized = 1f - ((y - sliderTopY) / sliderHeight).coerceIn(0f, 1f)
                                                val gainDb = (normalized * 24f) - 12f
                                                if (i == 0) {
                                                    onPreampChange(gainDb)
                                                } else {
                                                    onBandChange(i - 1, gainDb)
                                                }
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
                .pointerInput(isShade) {
                    if (isShade) return@pointerInput
                    detectDragGestures { change, _ ->
                        val x = change.position.x / winampScaleX
                        val y = change.position.y / winampScaleY

                        // Find which slider the drag belongs to
                        for (i in sliderXPositions.indices) {
                            val sx = sliderXPositions[i]
                            // Touch hitbox: 18px width around slider
                            if (x in (sx - 9f)..(sx + 20f) && y in (sliderTopY - 10f)..(sliderTopY + sliderHeight + 10f)) {
                                val normalized = 1f - ((y - sliderTopY) / sliderHeight).coerceIn(0f, 1f)
                                val gainDb = (normalized * 24f) - 12f // -12dB to +12dB

                                if (i == 0) {
                                    onPreampChange(gainDb)
                                } else {
                                    onBandChange(i - 1, gainDb)
                                }
                                break
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 275f
                val scaleY = size.height / (if (isShade) 14f else 116f)

                // 0. Render EQ Windowshade Mode (275 x 14 px)
                if (isShade) {
                    if (eqMainImg != null) {
                        drawImage(
                            image = eqMainImg,
                            srcOffset = IntOffset(0, 134),
                            srcSize = IntSize(275, 14),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                        // Shade toggle button
                        drawImage(
                            image = eqMainImg,
                            srcOffset = IntOffset(0, 149),
                            srcSize = IntSize(9, 9),
                            dstOffset = IntOffset((252 * scaleX).toInt(), (3 * scaleY).toInt()),
                            dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                        )
                        // Close button
                        drawImage(
                            image = eqMainImg,
                            srcOffset = IntOffset(9, 149),
                            srcSize = IntSize(9, 9),
                            dstOffset = IntOffset((264 * scaleX).toInt(), (3 * scaleY).toInt()),
                            dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                        )
                    }

                    // Draw miniature live EQ curve on the right (Zero allocation, reusing shadeEqPath)
                    if (isEqEnabled && bandsDb.isNotEmpty()) {
                        shadeEqPath.reset()
                        val graphLeft = 60f * scaleX
                        val graphW = 180f * scaleX
                        val graphMidY = 7f * scaleY
                        val graphAmp = 5f * scaleY

                        val count = bandsDb.size
                        val denom = (count - 1).coerceAtLeast(1)
                        for (i in 0 until count) {
                            val px = graphLeft + (i.toFloat() / denom) * graphW
                            val py = graphMidY - (bandsDb[i] / 12f) * graphAmp
                            if (i == 0) {
                                shadeEqPath.moveTo(px, py)
                            } else {
                                val prevX = graphLeft + ((i - 1).toFloat() / denom) * graphW
                                val prevY = graphMidY - (bandsDb[i - 1] / 12f) * graphAmp
                                val cx = (prevX + px) / 2f
                                shadeEqPath.quadraticTo(prevX, prevY, cx, (prevY + py) / 2f)
                            }
                        }
                        val lastX = graphLeft + graphW
                        val lastY = graphMidY - (bandsDb.last() / 12f) * graphAmp
                        shadeEqPath.lineTo(lastX, lastY)
                        drawPath(shadeEqPath, color = Color(0xFFFFFF00), style = Stroke(width = 1.5f * scaleX))
                    }
                    return@Canvas
                }

                // 1. Draw EQ Background (EQMAIN.BMP)
                if (eqMainImg != null) {
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(275, 116),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )

                    // 2. Draw WINAMP EQUALIZER Titlebar (active: 0, 134, 275, 14)
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(0, 134),
                        srcSize = IntSize(275, 14),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(size.width.toInt(), (14 * scaleY).toInt())
                    )

                    // Close button at (264, 3)
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(0, 116),
                        srcSize = IntSize(9, 9),
                        dstOffset = IntOffset((264 * scaleX).toInt(), (3 * scaleY).toInt()),
                        dstSize = IntSize((9 * scaleX).toInt(), (9 * scaleY).toInt())
                    )

                    // 3. Draw ON Button at (14, 18)
                    // Normal: (10, 119, 26, 12), Active: (69, 119, 26, 12)
                    val onSrcX = if (isEqEnabled) 69 else 10
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(onSrcX, 119),
                        srcSize = IntSize(26, 12),
                        dstOffset = IntOffset((14 * scaleX).toInt(), (18 * scaleY).toInt()),
                        dstSize = IntSize((26 * scaleX).toInt(), (12 * scaleY).toInt())
                    )

                    // 4. Draw AUTO Button at (39, 18)
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(36, 119),
                        srcSize = IntSize(32, 12),
                        dstOffset = IntOffset((39 * scaleX).toInt(), (18 * scaleY).toInt()),
                        dstSize = IntSize((32 * scaleX).toInt(), (12 * scaleY).toInt())
                    )

                    // 5. Draw PRESETS Button at (217, 18)
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(224, 164),
                        srcSize = IntSize(44, 12),
                        dstOffset = IntOffset((217 * scaleX).toInt(), (18 * scaleY).toInt()),
                        dstSize = IntSize((44 * scaleX).toInt(), (12 * scaleY).toInt())
                    )

                    // 6. Draw EQ Graph Spline Window at (86, 17, 113, 19)
                    drawImage(
                        image = eqMainImg,
                        srcOffset = IntOffset(0, 294),
                        srcSize = IntSize(113, 19),
                        dstOffset = IntOffset((86 * scaleX).toInt(), (17 * scaleY).toInt()),
                        dstSize = IntSize((113 * scaleX).toInt(), (19 * scaleY).toInt())
                    )
                }

                // 7. Draw EQ Spline Curve (reusing normalEqPath, zero heap allocation)
                val graphX = 86f * scaleX
                val graphY = 17f * scaleY
                val graphW = 113f * scaleX
                val graphH = 19f * scaleY
                val graphMidY = graphY + (graphH / 2f)

                if (bandsDb.isNotEmpty()) {
                    normalEqPath.reset()
                    for (b in 0 until 10) {
                        val gain = bandsDb.getOrElse(b) { 0f }.coerceIn(-12f, 12f)
                        val norm = gain / 12f // -1 to +1
                        val px = graphX + (b.toFloat() / 9f) * graphW
                        val py = graphMidY - (norm * (graphH / 2f - 2f))

                        if (b == 0) {
                            normalEqPath.moveTo(px, py)
                        } else {
                            val prevGain = bandsDb.getOrElse(b - 1) { 0f }.coerceIn(-12f, 12f)
                            val prevNorm = prevGain / 12f
                            val prevX = graphX + ((b - 1).toFloat() / 9f) * graphW
                            val prevY = graphMidY - (prevNorm * (graphH / 2f - 2f))
                            val midX = (prevX + px) / 2f
                            normalEqPath.cubicTo(midX, prevY, midX, py, px, py)
                        }
                    }
                    drawPath(
                        path = normalEqPath,
                        color = Color(0xFFE8E82E),
                        style = Stroke(width = 1.5f * scaleX)
                    )
                }

                // 8. Draw Yellow Groove Tracks and Metallic Handles for Preamp + 10 Bands
                for (i in sliderXPositions.indices) {
                    val gain = if (i == 0) preampDb else bandsDb.getOrElse(i - 1) { 0f }
                    val normalized = ((gain + 12f) / 24f).coerceIn(0f, 1f)
                    val thumbY = sliderTopY + (1f - normalized) * (sliderHeight - 11f)
                    val thumbX = sliderXPositions[i]

                    // Yellow track line
                    drawLine(
                        color = Color(0xFFC8C828),
                        start = Offset(thumbX * scaleX + (5.5f * scaleX), sliderTopY * scaleY),
                        end = Offset(thumbX * scaleX + (5.5f * scaleX), (sliderTopY + sliderHeight) * scaleY),
                        strokeWidth = 1.5f * scaleX
                    )
                    // Track shadow line
                    drawLine(
                        color = Color(0xFF404010),
                        start = Offset(thumbX * scaleX + (6.5f * scaleX), sliderTopY * scaleY),
                        end = Offset(thumbX * scaleX + (6.5f * scaleX), (sliderTopY + sliderHeight) * scaleY),
                        strokeWidth = 1f * scaleX
                    )

                    // Metallic slider handle from EQMAIN.BMP (0, 164, 11, 11)
                    if (eqMainImg != null) {
                        drawImage(
                            image = eqMainImg,
                            srcOffset = IntOffset(0, 164),
                            srcSize = IntSize(11, 11),
                            dstOffset = IntOffset((thumbX * scaleX).toInt(), (thumbY * scaleY).toInt()),
                            dstSize = IntSize((11 * scaleX).toInt(), (11 * scaleY).toInt())
                        )
                    } else {
                        drawRect(
                            color = Color(0xFFCCCCCC),
                            topLeft = Offset(thumbX * scaleX, thumbY * scaleY),
                            size = androidx.compose.ui.geometry.Size(11f * scaleX, 11f * scaleY)
                        )
                    }
                }
            }
        }
    }
}
