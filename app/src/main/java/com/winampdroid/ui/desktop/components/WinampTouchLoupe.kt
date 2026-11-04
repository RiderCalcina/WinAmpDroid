package com.winampdroid.ui.desktop.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Data model for the interactive Touch Loupe magnifier.
 * Activates when long-pressing micro-buttons designed for mouse pointers.
 */
data class LoupeInfo(
    val isVisible: Boolean = false,
    val touchPosition: Offset = Offset.Zero,
    val buttonName: String = "",
    val buttonLabel: String = "",
    val hotkeyLabel: String = ""
)

/**
 * Winamp Touch Loupe Layer.
 * Floating retro magnifying glass that appears directly above the user's finger,
 * displaying a 2.5x zoomed view and clear high-contrast labels for Winamp's classic micro-buttons.
 */
@Composable
fun WinampTouchLoupeLayer(
    loupeInfo: LoupeInfo,
    modifier: Modifier = Modifier
) {
    if (!loupeInfo.isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(9999f)
    ) {
        // Position the loupe lens 80dp above the finger so it's not occluded
        val loupeX = loupeInfo.touchPosition.x - 60f
        val loupeY = (loupeInfo.touchPosition.y - 120f).coerceAtLeast(20f)

        AnimatedVisibility(
            visible = loupeInfo.isVisible,
            enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) + scaleIn(spring(stiffness = Spring.StiffnessHigh)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) + scaleOut(spring(stiffness = Spring.StiffnessHigh)),
            modifier = Modifier.offset { IntOffset(loupeX.toInt(), loupeY.toInt()) }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. The Circular / Stadium Magnifying Lens
                Box(
                    modifier = Modifier
                        .size(120.dp, 80.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2E2E3E),
                                    Color(0xFF141420),
                                    Color(0xFF0A0A10)
                                )
                            )
                        )
                        .border(2.dp, Color(0xFF6B7280), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Precision Crosshair and Glow
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f

                        // Crosshair circle
                        drawCircle(
                            color = Color(0x3300FF00),
                            radius = 24.dp.toPx(),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Center crosshair lines
                        drawLine(
                            color = Color(0xFF00FF00),
                            start = Offset(cx - 15.dp.toPx(), cy),
                            end = Offset(cx + 15.dp.toPx(), cy),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF00FF00),
                            start = Offset(cx, cy - 12.dp.toPx()),
                            end = Offset(cx, cy + 12.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Button details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = loupeInfo.buttonLabel,
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (loupeInfo.hotkeyLabel.isNotEmpty()) {
                            Text(
                                text = loupeInfo.hotkeyLabel,
                                color = Color(0xFFFFFF00),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 2. Connector Stalk pointing down toward the finger
                Canvas(
                    modifier = Modifier
                        .width(16.dp)
                        .height(14.dp)
                ) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    }
                    drawPath(path, color = Color(0xFF6B7280))
                }
            }
        }
    }
}

