package com.winampdroid.ui.desktop.components

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.winampdroid.core.skin.WinampSkin

/**
 * Pixel-faithful rendering of Winamp's Milkdrop / Butterchurn Window (275 x 150 px).
 * Matches the official Webamp / Winamp GenWindow specifications:
 * - Authentic 20px high Titlebar with GEN.BMP chrome, centered "MILKDROP" text and close button
 * - Authentic 11px wide Left Border (GEN_MIDDLE_LEFT) and 8px wide Right Border (GEN_MIDDLE_RIGHT)
 * - Authentic 14px high Bottom Border (GEN_BOTTOM_LEFT and GEN_BOTTOM_RIGHT)
 * - WebGL 2.0 / GLSL Warp Feedback Shader running offline at 60 FPS (Butterchurn / Milkdrop engine)
 * - Music-reactive audio physics (bass, mid, treble modulating zoom, rot, warp, color palette)
 * - Swipe horizontal or tap visualizer to navigate presets (anterior / siguiente)
 * - Double tap or maximize button for Immersive Fullscreen Milkdrop Experience
 * - Direct Picture-in-Picture (PiP) launcher integration
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MilkdropWindowView(
    skin: WinampSkin?,
    trackTitle: String = "WINAMP",
    isPlaying: Boolean,
    isPaused: Boolean = false,
    spectralData: FloatArray = FloatArray(0),
    pcmWaveData: ShortArray = ShortArray(0),
    visualizerDataProvider: (() -> com.winampdroid.ui.desktop.VisualizerData)? = null,
    milkdropBridge: MilkdropBridge? = null,
    onClose: () -> Unit,
    isShade: Boolean = false,
    onToggleShade: () -> Unit = {},
    onEnterPip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val genBmp = skin?.getBitmap("GEN")
    val genImg = remember(genBmp) { genBmp?.asImageBitmap() }
    val titlebarBmp = skin?.getBitmap("TITLEBAR")
    val titlebarImg = remember(titlebarBmp) { titlebarBmp?.asImageBitmap() }

    val context = LocalContext.current
    val fallbackBridge = remember { MilkdropBridge() }
    val activeBridge = milkdropBridge ?: fallbackBridge

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var fullscreenWebViewRef by remember { mutableStateOf<WebView?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    val isStopped = !isPlaying && !isPaused

    var dockedDragX by remember { mutableFloatStateOf(0f) }
    var fsDragX by remember { mutableFloatStateOf(0f) }

    // Winamp authentic behavior: When track starts playing or track changes, show song title in center for ~2.5s and fade out
    LaunchedEffect(trackTitle, isPlaying) {
        if (isPlaying && trackTitle.isNotBlank() && trackTitle != "WINAMP") {
            val safeTitle = trackTitle.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ")
            val js = "if(window.showSongTitle) window.showSongTitle('$safeTitle');"
            webViewRef?.evaluateJavascript(js, null)
            fullscreenWebViewRef?.evaluateJavascript(js, null)
        }
    }

    // Single-shot state notification to Milkdrop engine (Zero continuous IPC loop!)
    LaunchedEffect(isPlaying, isPaused, isShade, isFullscreen) {
        activeBridge.updateState(isPlaying && !isShade, isPaused)
        val targets = listOfNotNull(webViewRef, if (isFullscreen) fullscreenWebViewRef else null)
        if (targets.isEmpty()) return@LaunchedEffect

        when {
            isStopped || isShade -> {
                val js = "if(window.stopMilkdrop) window.stopMilkdrop();"
                targets.forEach { it.evaluateJavascript(js, null) }
            }
            isPaused -> {
                val js = "if(window.pauseMilkdrop) window.pauseMilkdrop();"
                targets.forEach { it.evaluateJavascript(js, null) }
            }
            isPlaying -> {
                val js = "if(window.startMilkdrop) window.startMilkdrop();"
                targets.forEach { it.evaluateJavascript(js, null) }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
    ) {
        val scale = maxWidth / 275.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isShade) Modifier.height(20.dp * scale)
                    else Modifier.fillMaxHeight()
                )
                .background(Color.Black)
        ) {
            // 1. Skinned MILKDROP Titlebar (20px high in Winamp / Webamp GenWindow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp * scale)
                    .background(Color.Black)
                    .pointerInput(isShade) {
                        detectTapGestures(
                            onDoubleTap = { onToggleShade() },
                            onTap = { offset ->
                                val scaleX = size.width / 275f
                                val x = offset.x / scaleX
                                when {
                                    // Shade button tap area
                                    x in 242f..257f -> onToggleShade()
                                    // Fullscreen button tap area
                                    x in 226f..241f -> { isFullscreen = true }
                                    else -> {
                                        // Tap titlebar to cycle presets
                                        webViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                                    }
                                }
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / 275f

                    if (genImg != null) {
                        val leftW = (25 * scaleX).toInt()
                        val rightW = (25 * scaleX).toInt()
                        val fillW = (size.width - leftW - rightW).toInt()

                        // GEN_TOP_LEFT_SELECTED: (x=0, y=0, w=25, h=20)
                        drawImage(
                            image = genImg,
                            srcOffset = IntOffset(0, 0),
                            srcSize = IntSize(25, 20),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(leftW, size.height.toInt())
                        )

                        // GEN_TOP_LEFT_RIGHT_FILL_SELECTED: (x=105, y=0, w=23, h=20) - Relleno continuo limpio sin líneas divisorias de plantilla
                        if (fillW > 0) {
                            drawImage(
                                image = genImg,
                                srcOffset = IntOffset(105, 0),
                                srcSize = IntSize(23, 20),
                                dstOffset = IntOffset(leftW, 0),
                                dstSize = IntSize(fillW, size.height.toInt())
                            )
                        }

                        // GEN_TOP_RIGHT_SELECTED: (x=130, y=0, w=25, h=20)
                        drawImage(
                            image = genImg,
                            srcOffset = IntOffset(130, 0),
                            srcSize = IntSize(25, 20),
                            dstOffset = IntOffset((size.width - rightW).toInt(), 0),
                            dstSize = IntSize(rightW, size.height.toInt())
                        )
                    } else if (titlebarImg != null) {
                        drawImage(
                            image = titlebarImg,
                            srcOffset = IntOffset(27, 0),
                            srcSize = IntSize(275, 14),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    } else {
                        drawRect(Color(0xFF333333))
                    }
                }
            }

            if (!isShade) {
                // 2. Middle Section: Authentic Skinned Borders + Hardware-Accelerated WebGL Milkdrop Canvas
                // Takes weight(1f) to occupy all empty space down to the navigation buttons!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Skinned Left Border from GEN_MIDDLE_LEFT (x=127, y=42, w=11, h=29)
                    Box(
                        modifier = Modifier
                            .width(11.dp * scale)
                            .fillMaxHeight()
                            .background(Color.Black)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (genImg != null) {
                                var curY = 0
                                val tileH = (29 * (size.width / 11f)).toInt().coerceAtLeast(1)
                                while (curY < size.height.toInt()) {
                                    val h = minOf(tileH, size.height.toInt() - curY)
                                    drawImage(
                                        image = genImg,
                                        srcOffset = IntOffset(127, 42),
                                        srcSize = IntSize(10, 29),
                                        dstOffset = IntOffset(0, curY),
                                        dstSize = IntSize(size.width.toInt(), h)
                                    )
                                    curY += h
                                }
                            } else {
                                drawRect(Color(0xFF222222))
                            }
                        }
                    }

                    // Hardware-Accelerated WebGL Milkdrop Shader Visualizer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    setBackgroundColor(AndroidColor.BLACK)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = true
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE

                                    // Pull-based AndroidBridge: WebGL pulls audio directly on rAF (Zero Looper flood!)
                                    addJavascriptInterface(activeBridge, "AndroidBridge")

                                    webChromeClient = object : android.webkit.WebChromeClient() {
                                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                            return true
                                        }
                                    }
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            when {
                                                isStopped || isShade -> view?.evaluateJavascript("if(window.stopMilkdrop) window.stopMilkdrop();", null)
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
                            onRelease = { view ->
                                try {
                                    view.stopLoading()
                                    view.destroy()
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Transparent Gesture Overlay on top of AndroidView
                        // Captures Tap (next preset), Double Tap (fullscreen), and Horizontal Drag (swipe presets)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            if (dockedDragX > 40f) {
                                                webViewRef?.evaluateJavascript("if(window.prevPreset) window.prevPreset();", null)
                                            } else if (dockedDragX < -40f) {
                                                webViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                                            }
                                            dockedDragX = 0f
                                        },
                                        onDragCancel = { dockedDragX = 0f }
                                    ) { _, dragAmount ->
                                        dockedDragX += dragAmount
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            webViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                                        },
                                        onDoubleTap = {
                                            isFullscreen = true
                                        }
                                    )
                                }
                        )
                    }

                    // Skinned Right Border from GEN_MIDDLE_RIGHT (x=139, y=42, w=8, h=29)
                    Box(
                        modifier = Modifier
                            .width(8.dp * scale)
                            .fillMaxHeight()
                            .background(Color.Black)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (genImg != null) {
                                var curY = 0
                                val tileH = (29 * (size.width / 8f)).toInt().coerceAtLeast(1)
                                while (curY < size.height.toInt()) {
                                    val h = minOf(tileH, size.height.toInt() - curY)
                                    drawImage(
                                        image = genImg,
                                        srcOffset = IntOffset(139, 42),
                                        srcSize = IntSize(7, 29),
                                        dstOffset = IntOffset(0, curY),
                                        dstSize = IntSize(size.width.toInt(), h)
                                    )
                                    curY += h
                                }
                            } else {
                                drawRect(Color(0xFF222222))
                            }
                        }
                    }
                }

                // 3. Skinned Bottom Border (14px high)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp * scale)
                        .background(Color.Black)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val scaleX = size.width / 275f

                        if (genImg != null) {
                            val leftW = (125 * scaleX).toInt()
                            val rightW = (125 * scaleX).toInt()
                            val fillW = (size.width - leftW - rightW).toInt()

                            // GEN_BOTTOM_LEFT: (x=0, y=42, w=124, h=14)
                            drawImage(
                                image = genImg,
                                srcOffset = IntOffset(0, 42),
                                srcSize = IntSize(124, 14),
                                dstOffset = IntOffset(0, 0),
                                dstSize = IntSize(leftW, size.height.toInt())
                            )

                            // Center bottom fill if wide window (x=128, y=72, w=22, h=14)
                            if (fillW > 0) {
                                drawImage(
                                    image = genImg,
                                    srcOffset = IntOffset(128, 72),
                                    srcSize = IntSize(22, 14),
                                    dstOffset = IntOffset(leftW, 0),
                                    dstSize = IntSize(fillW, size.height.toInt())
                                )
                            }

                            // GEN_BOTTOM_RIGHT: (x=0, y=57, w=124, h=14)
                            drawImage(
                                image = genImg,
                                srcOffset = IntOffset(0, 57),
                                srcSize = IntSize(124, 14),
                                dstOffset = IntOffset((size.width - rightW).toInt(), 0),
                                dstSize = IntSize(rightW, size.height.toInt())
                            )
                        } else {
                            drawRect(Color(0xFF222222))
                        }
                    }
                }
            }
        }
    }

    // 4. Immersive Fullscreen Milkdrop Experience (Zero HUD, Zero Windows, Keep Screen ON)
    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dialogView = LocalView.current

            // Prevent screen from dimming or turning off during fullscreen playback
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
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
                            addJavascriptInterface(activeBridge, "AndroidBridge")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    when {
                                        isStopped || isShade -> view?.evaluateJavascript("if(window.stopMilkdrop) window.stopMilkdrop();", null)
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
                            fullscreenWebViewRef = this
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

                // Transparent Gesture Overlay for Fullscreen Mode
                // Captures Tap (next preset), Double Tap (exit fullscreen), and Horizontal Drag (swipe presets)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (fsDragX > 50f) {
                                        fullscreenWebViewRef?.evaluateJavascript("if(window.prevPreset) window.prevPreset();", null)
                                    } else if (fsDragX < -50f) {
                                        fullscreenWebViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                                    }
                                    fsDragX = 0f
                                },
                                onDragCancel = { fsDragX = 0f }
                            ) { _, dragAmount ->
                                fsDragX += dragAmount
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { isFullscreen = false },
                                onTap = {
                                    fullscreenWebViewRef?.evaluateJavascript("if(window.nextPreset) window.nextPreset();", null)
                                }
                            )
                        }
                )
            }
        }
    }
}
