package com.winampdroid.core.skin

import android.graphics.Bitmap
import android.graphics.Color
import java.io.InputStream
import java.util.zip.ZipInputStream

data class SkinColors(
    val visColors: List<Int> = defaultVisColors(),
    val plNormalText: Int = Color.rgb(0, 255, 0),
    val plCurrentText: Int = Color.rgb(255, 255, 0),
    val plNormalBg: Int = Color.BLACK,
    val plSelectedBg: Int = Color.rgb(0, 0, 136)
) {
    companion object {
        fun defaultVisColors(): List<Int> {
            // 24 standard Winamp visualizer colors
            return listOf(
                Color.BLACK, // 0: Background
                Color.rgb(0, 255, 0), // 1: Dots
                // 2-17: 16 spectrum bar colors (from top to bottom: red -> yellow -> green)
                Color.rgb(217, 34, 0),
                Color.rgb(213, 56, 0),
                Color.rgb(212, 79, 0),
                Color.rgb(211, 101, 0),
                Color.rgb(211, 124, 0),
                Color.rgb(210, 147, 0),
                Color.rgb(209, 169, 0),
                Color.rgb(209, 192, 0),
                Color.rgb(208, 215, 0),
                Color.rgb(186, 214, 0),
                Color.rgb(164, 214, 0),
                Color.rgb(141, 214, 0),
                Color.rgb(119, 214, 0),
                Color.rgb(96, 214, 0),
                Color.rgb(74, 214, 0),
                Color.rgb(51, 214, 0),
                Color.rgb(255, 255, 255), // 18: Peak dots
                Color.rgb(0, 255, 0),     // 19: Oscilloscope line 1
                Color.rgb(0, 200, 0),     // 20: Oscilloscope line 2
                Color.rgb(0, 160, 0),     // 21: Oscilloscope line 3
                Color.rgb(0, 120, 0),     // 22: Oscilloscope line 4
                Color.rgb(0, 80, 0)       // 23: Oscilloscope line 5
            )
        }
    }
}

class WinampSkin(
    val bitmaps: Map<String, Bitmap>,
    val colors: SkinColors,
    var fallbackSkin: WinampSkin? = null
) {
    fun getBitmap(name: String): Bitmap? = bitmaps[name.uppercase()] ?: fallbackSkin?.getBitmap(name)
}

object WszSkinParser {
    fun parse(inputStream: InputStream, fallbackSkin: WinampSkin? = null): WinampSkin {
        val bitmaps = mutableMapOf<String, Bitmap>()
        var visColorLines = emptyList<String>()
        var pleditLines = emptyList<String>()

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/').uppercase()
                if (name.endsWith(".BMP")) {
                    val key = name.substringAfterLast("/").removeSuffix(".BMP")
                    val bytes = zip.readBytes()
                    val rawBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (rawBitmap != null) {
                        bitmaps[key] = sanitizeGuidelines(key, rawBitmap)
                    }
                } else if (name.endsWith("VISCOLOR.TXT")) {
                    val text = String(zip.readBytes(), Charsets.ISO_8859_1)
                    visColorLines = text.lines()
                } else if (name.endsWith("PLEDIT.TXT")) {
                    val text = String(zip.readBytes(), Charsets.ISO_8859_1)
                    pleditLines = text.lines()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val plColors = parsePleditColors(pleditLines)
        val colors = SkinColors(
            visColors = if (visColorLines.isNotEmpty()) parseVisColors(visColorLines).visColors else SkinColors.defaultVisColors(),
            plNormalText = plColors["normal"] ?: Color.rgb(0, 255, 0),
            plCurrentText = plColors["current"] ?: Color.rgb(255, 255, 0),
            plNormalBg = plColors["normalbg"] ?: Color.BLACK,
            plSelectedBg = plColors["selectedbg"] ?: Color.rgb(0, 0, 136)
        )

        return WinampSkin(bitmaps, colors, fallbackSkin)
    }

    private fun parsePleditColors(lines: List<String>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (line in lines) {
            val trimmed = line.substringBefore("//").substringBefore(";").trim()
            if (trimmed.startsWith("#") || !trimmed.contains("=")) continue
            val parts = trimmed.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                val parsedColor = parseHexOrRgbColor(value)
                if (parsedColor != null) {
                    result[key] = parsedColor
                }
            }
        }
        return result
    }

    private fun parseHexOrRgbColor(str: String): Int? {
        return try {
            if (str.startsWith("#")) {
                val hex = str.removePrefix("#")
                if (hex.length == 6) {
                    Color.rgb(
                        hex.substring(0, 2).toInt(16),
                        hex.substring(2, 4).toInt(16),
                        hex.substring(4, 6).toInt(16)
                    )
                } else null
            } else {
                val parts = str.split(Regex("[,\\s]+"))
                if (parts.size >= 3) {
                    Color.rgb(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVisColors(lines: List<String>): SkinColors {
        val parsed = mutableListOf<Int>()
        for (line in lines) {
            val trimmed = line.substringBefore("//").substringBefore(";").trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(Regex("[,\\s]+")).filter { it.isNotEmpty() }
            if (parts.size >= 3) {
                val r = parts[0].toIntOrNull() ?: 0
                val g = parts[1].toIntOrNull() ?: 0
                val b = parts[2].toIntOrNull() ?: 0
                parsed.add(Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255)))
            }
        }
        return if (parsed.size >= 24) SkinColors(parsed) else SkinColors()
    }

    private fun isCyanGuideline(r: Int, g: Int, b: Int): Boolean {
        // Broad detection for Winamp skin template cyan separators (#00C6FF, #1ACCFF, etc.)
        return (r < 65 && g in 140..255 && b in 180..255) || (r < 50 && g > 150 && b > 190)
    }

    private fun isMagentaMask(r: Int, g: Int, b: Int): Boolean {
        // Magenta corner mask (255, 0, 255)
        return (r in 220..255 && g < 40 && b in 220..255)
    }

    /**
     * Sanitizes Winamp skin bitmaps by removing template guidelines, separator boundary columns,
     * and magenta transparency masks.
     *
     * In Winamp 2.x skins, BMPs use 1-pixel separator lines (cyan #00C6FF, white #FFFFFF, red, etc.)
     * to demarcate sprite boundaries in official Nullsoft templates (PLEDIT, GEN). When scaled on
     * modern mobile screens, these border pixels bleed into neighboring tiles. This method neutralizes
     * separator columns and replaces guidelines with adjacent interior pixels.
     */
    private fun sanitizeGuidelines(key: String, source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        var modified = false

        // 1. Template guideline column cleanse for PLEDIT and GEN bitmaps
        if (key == "PLEDIT") {
            // Top section separator columns (y in 0 until 42): 25, 126, 152, 178, 204
            val topCols = intArrayOf(25, 126, 152, 178, 204)
            for (col in topCols) {
                if (col in 1 until width) {
                    for (y in 0 until minOf(height, 42)) {
                        pixels[y * width + col] = pixels[y * width + (col - 1)]
                        modified = true
                    }
                }
            }
            // Middle section separator columns (y in 42 until 72): 12, 51
            val midCols = intArrayOf(12, 51)
            for (col in midCols) {
                if (col in 1 until width) {
                    for (y in 42 until minOf(height, 72)) {
                        pixels[y * width + col] = pixels[y * width + (col - 1)]
                        modified = true
                    }
                }
            }
            // Bottom section separator column (y in 72 until 115): 125
            if (125 in 1 until width) {
                for (y in 72 until minOf(height, 115)) {
                    pixels[y * width + 125] = pixels[y * width + 124]
                    modified = true
                }
            }
        } else if (key == "GEN") {
            // Top section separator columns (y in 0 until 42): 25, 51, 77, 103, 129
            val topCols = intArrayOf(25, 51, 77, 103, 129)
            for (col in topCols) {
                if (col in 1 until width) {
                    for (y in 0 until minOf(height, 42)) {
                        pixels[y * width + col] = pixels[y * width + (col - 1)]
                        modified = true
                    }
                }
            }
            // Middle section separator columns (y in 42 until 72): 138, 147
            val midCols = intArrayOf(138, 147)
            for (col in midCols) {
                if (col in 1 until width) {
                    for (y in 42 until minOf(height, 72)) {
                        pixels[y * width + col] = pixels[y * width + (col - 1)]
                        modified = true
                    }
                }
            }
            // Bottom section separator columns (y in 72 until 100): 126, 151, 152
            val botCols = intArrayOf(126, 151, 152)
            for (col in botCols) {
                if (col in 1 until width) {
                    for (y in 72 until minOf(height, 100)) {
                        pixels[y * width + col] = pixels[y * width + (col - 1)]
                        modified = true
                    }
                }
            }
        }

        // 2. Color-based guideline and mask sanitization
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            if (isMagentaMask(r, g, b)) {
                pixels[i] = Color.TRANSPARENT
                modified = true
            } else if (isCyanGuideline(r, g, b)) {
                val x = i % width
                val y = i / width
                var replacement = Color.TRANSPARENT

                // Search left for nearest valid non-guideline pixel
                var step = 1
                while (x - step >= 0 && step <= 10) {
                    val c = pixels[y * width + (x - step)]
                    val cr = (c shr 16) and 0xFF
                    val cg = (c shr 8) and 0xFF
                    val cb = c and 0xFF
                    if (!isCyanGuideline(cr, cg, cb) && !isMagentaMask(cr, cg, cb)) {
                        replacement = c
                        break
                    }
                    step++
                }

                // If not found left, search right
                if (replacement == Color.TRANSPARENT) {
                    step = 1
                    while (x + step < width && step <= 10) {
                        val c = pixels[y * width + (x + step)]
                        val cr = (c shr 16) and 0xFF
                        val cg = (c shr 8) and 0xFF
                        val cb = c and 0xFF
                        if (!isCyanGuideline(cr, cg, cb) && !isMagentaMask(cr, cg, cb)) {
                            replacement = c
                            break
                        }
                        step++
                    }
                }

                // If not found horizontally, search vertically (up/down)
                if (replacement == Color.TRANSPARENT) {
                    step = 1
                    while (y - step >= 0 && step <= 10) {
                        val c = pixels[(y - step) * width + x]
                        val cr = (c shr 16) and 0xFF
                        val cg = (c shr 8) and 0xFF
                        val cb = c and 0xFF
                        if (!isCyanGuideline(cr, cg, cb) && !isMagentaMask(cr, cg, cb)) {
                            replacement = c
                            break
                        }
                        step++
                    }
                }

                pixels[i] = replacement
                modified = true
            }
        }

        return if (modified) {
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            output.setPixels(pixels, 0, width, 0, 0, width, height)
            if (!source.isRecycled) {
                source.recycle()
            }
            output
        } else {
            source
        }
    }
}

