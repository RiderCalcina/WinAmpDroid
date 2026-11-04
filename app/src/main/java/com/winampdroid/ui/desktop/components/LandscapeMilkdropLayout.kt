package com.winampdroid.ui.desktop.components

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Landscape Milkdrop Experience (Modo Horizontal).
 *
 * Se activa automáticamente al rotar el dispositivo celular a posición horizontal.
 * Ocupa el 100% de la pantalla panorámica con el motor WebGL Milkdrop a 60 FPS,
 * manteniendo la pantalla encendida y ofreciendo controles retro flotantes
 * para la reproducción de música sin salir del modo panorámico.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LandscapeMilkdropLayout(
    trackTitle: String,
    isPlaying: Boolean,
    isPaused: Boolean,
    timeElapsedSeconds: Int,
    totalDurationSeconds: Int,
    milkdropBridge: MilkdropBridge,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isHudVisible by remember { mutableStateOf(true) }
    var dragX by remember { mutableFloatStateOf(0f) }

    // Keep screen on in landscape while playing
    DisposableEffect(isPlaying) {
        val activity = context as? Activity
        if (isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-advance song title toast in Milkdrop on track transition
    LaunchedEffect(trackTitle, isPlaying) {
        if (isPlaying && trackTitle.isNotBlank() && trackTitle != "WINAMP") {
            val safeTitle = trackTitle.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
            webViewRef?.evaluateJavascript("if(window.showSongTitle) window.showSongTitle('$safeTitle');", null)
        }
    }

    // Single-shot state notification (Zero IPC polling loop!)
    LaunchedEffect(isPlaying, isPaused) {
        milkdropBridge.updateState(isPlaying, isPaused)
        val view = webViewRef ?: return@LaunchedEffect
        when {
            !isPlaying && !isPaused -> view.evaluateJavascript("if(window.stopMilkdrop) window.stopMilkdrop();", null)
            isPaused -> view.evaluateJavascript("if(window.pauseMilkdrop) window.pauseMilkdrop();", null)
            isPlaying -> view.evaluateJavascript("if(window.startMilkdrop) window.startMilkdrop();", null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Hardware Accelerated WebGL Milkdrop Canvas
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setBackgroundColor(AndroidColor.BLACK)
                    keepScreenOn = isPlaying
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE

                    // Pull-based AndroidBridge: WebGL pulls audio directly on rAF (Zero Looper flood!)
                    addJavascriptInterface(milkdropBridge, "AndroidBridge")

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (isPlaying && !isPaused) {
                                view?.evaluateJavascript("if(window.startMilkdrop) window.startMilkdrop();", null)
                            }
                            if (isPlaying && trackTitle.isNotBlank() && trackTitle != "WINAMP") {
                                val safeTitle = trackTitle.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
                                view?.evaluateJavascript("if(window.showSongTitle) window.showSongTitle('$safeTitle');", null)
                            }
                        }
                    }

                    val htmlContent = try {
                        ctx.assets.open("milkdrop/index.html").bufferedReader().use { it.readText() }
                    } catch (e: Exception) {
                        ""
                    }
                    loadDataWithBaseURL("https://localhost/", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            onRelease = { view ->
                try {
                    view.stopLoading()
                    view.destroy()
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gesture Overlay across entire screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX > 50f) {
                                webViewRef?.evaluateJavascript("if(window.prevPreset) window.prevPreset();", null)
                            } else if (dragX < -50f) {
                                webViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                            }
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f }
                    ) { _, dragAmount ->
                        dragX += dragAmount
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            isHudVisible = !isHudVisible
                        },
                        onDoubleTap = {
                            if (isPlaying) onPause() else onPlay()
                        }
                    )
                }
        )

        // 3. Floating Retro HUD (Auto-hide on tap)
        AnimatedVisibility(
            visible = isHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Header: Song Title & Preset Selector (con safe insets para notch y barras del sistema)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color(0xEE0D111A))
                        .border(BorderStroke(1.dp, Color(0x4400FF66)))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ ",
                            color = Color(0xFFFFB000),
                            fontSize = 14.sp
                        )
                        Text(
                            text = trackTitle,
                            color = Color(0xFF00FF66),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Surface(
                        onClick = {
                            webViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF1E2838),
                        border = BorderStroke(1.dp, Color(0xFF00FFCC))
                    ) {
                        Text(
                            text = "🎨 CAMBIAR PRESET",
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Bottom Floating Control Center (Controles CENTRADOS y con margen seguro contra la barra de navegación de Android)
                val curTimeStr = "%02d:%02d".format(timeElapsedSeconds / 60, timeElapsedSeconds % 60)
                val totTimeStr = "%02d:%02d".format(totalDurationSeconds / 60, totalDurationSeconds % 60)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xEE0D111A))
                        .border(BorderStroke(1.dp, Color(0x4400FF66)))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    // Time elapsed indicator alineado a la izquierda
                    Text(
                        text = "⏱ $curTimeStr / $totTimeStr",
                        color = Color(0xFF00FF66),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )

                    // Controles de transporte perfectamente CENTRADOS horizontalmente
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LandscapeRetroBtn(label = "|◀", onClick = onPrev)
                        if (isPlaying && !isPaused) {
                            LandscapeRetroBtn(label = "❚❚ PAUSA", onClick = onPause, isHighlight = true)
                        } else {
                            LandscapeRetroBtn(label = "▶ PLAY", onClick = onPlay, isHighlight = true)
                        }
                        LandscapeRetroBtn(label = "■ STOP", onClick = onStop)
                        LandscapeRetroBtn(label = "▶|", onClick = onNext)
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeRetroBtn(
    label: String,
    onClick: () -> Unit,
    isHighlight: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (isHighlight) Color(0xFF163820) else Color(0xFF1B2030),
        border = BorderStroke(1.dp, if (isHighlight) Color(0xFF00FF66) else Color(0xFF404868))
    ) {
        Text(
            text = label,
            color = if (isHighlight) Color(0xFF00FF66) else Color(0xFFD0D8F0),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

