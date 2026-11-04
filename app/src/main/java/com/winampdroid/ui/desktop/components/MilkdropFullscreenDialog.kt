package com.winampdroid.ui.desktop.components

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Fullscreen Milkdrop Visualizer Dialog.
 *
 * Se activa al hacer tap o doble tap sobre el contador digital de tiempo
 * o sobre las barras del visualizador de espectro en MainWindow.
 *
 * Características:
 * - 100% Pantalla Completa inmersiva a 60 FPS con motor WebGL Milkdrop.
 * - Conectado mediante MilkdropBridge (cero saturación de IPC).
 * - Gestos táctiles: Deslizar horizontalmente o tap para cambiar presets.
 * - Doble tap en cualquier parte de la pantalla o botón Atrás del sistema para cerrar.
 * - Mantiene la pantalla encendida (FLAG_KEEP_SCREEN_ON) durante la reproducción.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MilkdropFullscreenDialog(
    trackTitle: String,
    isPlaying: Boolean,
    isPaused: Boolean,
    milkdropBridge: MilkdropBridge,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val context = LocalContext.current
        val dialogView = LocalView.current

        // Mantener la pantalla encendida durante la reproducción a pantalla completa
        DisposableEffect(isPlaying) {
            val activity = context as? Activity
            if (isPlaying) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                try {
                    val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
                    dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } catch (_: Throwable) {}
            }
            onDispose {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                try {
                    val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
                    dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } catch (_: Throwable) {}
            }
        }

        var webViewRef by remember { mutableStateOf<WebView?>(null) }
        var dragX by remember { mutableFloatStateOf(0f) }

        // Marquesina flotante de título al cambiar de pista
        LaunchedEffect(trackTitle, isPlaying) {
            if (isPlaying && trackTitle.isNotBlank() && trackTitle != "WINAMP") {
                val safeTitle = trackTitle.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
                webViewRef?.evaluateJavascript("if(window.showSongTitle) window.showSongTitle('$safeTitle');", null)
            }
        }

        // Notificaciones puntuales de estado de reproducción
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. Hardware-Accelerated WebGL Canvas
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

                        // Puente pull nativo: WebGL lee energía de audio en rAF (Zero IPC flood!)
                        addJavascriptInterface(milkdropBridge, "AndroidBridge")

                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                when {
                                    !isPlaying && !isPaused -> view?.evaluateJavascript("if(window.stopMilkdrop) window.stopMilkdrop();", null)
                                    isPaused -> view?.evaluateJavascript("if(window.pauseMilkdrop) window.pauseMilkdrop();", null)
                                    isPlaying -> view?.evaluateJavascript("if(window.startMilkdrop) window.startMilkdrop();", null)
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
                update = { webView ->
                    webView.keepScreenOn = isPlaying
                },
                onRelease = { view ->
                    try {
                        view.stopLoading()
                        view.destroy()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Capa transparente para gestos táctiles (Tap, Doble Tap y Swipe horizontal)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragX > 40f) {
                                    webViewRef?.evaluateJavascript("if(window.prevPreset) window.prevPreset();", null)
                                } else if (dragX < -40f) {
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
                            onDoubleTap = { onDismissRequest() },
                            onTap = {
                                webViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                            }
                        )
                    }
            )
        }
    }
}

