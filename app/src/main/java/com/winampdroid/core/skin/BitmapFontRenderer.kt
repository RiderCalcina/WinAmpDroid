package com.winampdroid.core.skin

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF

/**
 * Renders marquee, track text and large digital LED numbers using Winamp's classic
 * bitmap font sheets (TEXT.BMP and NUMBERS.BMP).
 */
class BitmapFontRenderer(private val textBitmap: Bitmap?) {

    private val reusableSrcRect = Rect()
    private val reusableDstRect = RectF()
    private val reusableDigitSrcRect = Rect()
    private val reusableDigitDstRect = RectF()

    companion object {
        const val CHAR_WIDTH = 5
        const val CHAR_HEIGHT = 6

        const val DIGIT_WIDTH = 9
        const val DIGIT_HEIGHT = 13

        private val FONT_MAP = mapOf(
            'A' to Pair(0, 0), 'B' to Pair(0, 1), 'C' to Pair(0, 2), 'D' to Pair(0, 3),
            'E' to Pair(0, 4), 'F' to Pair(0, 5), 'G' to Pair(0, 6), 'H' to Pair(0, 7),
            'I' to Pair(0, 8), 'J' to Pair(0, 9), 'K' to Pair(0, 10), 'L' to Pair(0, 11),
            'M' to Pair(0, 12), 'N' to Pair(0, 13), 'O' to Pair(0, 14), 'P' to Pair(0, 15),
            'Q' to Pair(0, 16), 'R' to Pair(0, 17), 'S' to Pair(0, 18), 'T' to Pair(0, 19),
            'U' to Pair(0, 20), 'V' to Pair(0, 21), 'W' to Pair(0, 22), 'X' to Pair(0, 23),
            'Y' to Pair(0, 24), 'Z' to Pair(0, 25), '"' to Pair(0, 26), '@' to Pair(0, 27),
            ' ' to Pair(0, 30),
            '0' to Pair(1, 0), '1' to Pair(1, 1), '2' to Pair(1, 2), '3' to Pair(1, 3),
            '4' to Pair(1, 4), '5' to Pair(1, 5), '6' to Pair(1, 6), '7' to Pair(1, 7),
            '8' to Pair(1, 8), '9' to Pair(1, 9), '.' to Pair(1, 11), ':' to Pair(1, 12),
            '(' to Pair(1, 13), ')' to Pair(1, 14), '-' to Pair(1, 15), '\'' to Pair(1, 16),
            '!' to Pair(1, 17), '_' to Pair(1, 18), '+' to Pair(1, 19), '\\' to Pair(1, 20),
            '/' to Pair(1, 21), '[' to Pair(1, 22), ']' to Pair(1, 23), '^' to Pair(1, 24),
            '&' to Pair(1, 25), '%' to Pair(1, 26), ',' to Pair(1, 27), '=' to Pair(1, 28),
            '$' to Pair(1, 29), '#' to Pair(1, 30)
        )
    }

    fun drawText(
        canvas: Canvas,
        text: String,
        startX: Float,
        startY: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ) {
        if (textBitmap == null) return
        var curX = startX
        val upper = text.uppercase()

        for (ch in upper) {
            val pos = FONT_MAP[ch] ?: FONT_MAP[' ']!!
            val srcX = pos.second * CHAR_WIDTH
            val srcY = pos.first * CHAR_HEIGHT

            val srcRect = Rect(srcX, srcY, srcX + CHAR_WIDTH, srcY + CHAR_HEIGHT)
            val dstRect = RectF(
                curX,
                startY,
                curX + (CHAR_WIDTH * scaleX),
                startY + (CHAR_HEIGHT * scaleY)
            )

            canvas.drawBitmap(textBitmap, srcRect, dstRect, null)
            curX += (CHAR_WIDTH * scaleX)
        }
    }

    /**
     * Renders classic large digital time display (mm:ss) from NUMBERS.BMP.
     */
    fun drawDigitalTime(
        canvas: Canvas,
        numbersBitmap: Bitmap?,
        minutes: Int,
        seconds: Int,
        startX: Float,
        startY: Float,
        scaleX: Float = 1f,
        scaleY: Float = 1f
    ) {
        if (numbersBitmap == null) return

        val m1 = (minutes.coerceIn(0, 99) / 10)
        val m2 = (minutes.coerceIn(0, 99) % 10)
        val s1 = (seconds.coerceIn(0, 59) / 10)
        val s2 = (seconds.coerceIn(0, 59) % 10)

        // Digit 1 (Minute tens) at startX + 0
        drawDigit(canvas, numbersBitmap, m1, startX, startY, scaleX, scaleY)
        // Digit 2 (Minute ones) at startX + 12
        drawDigit(canvas, numbersBitmap, m2, startX + (12f * scaleX), startY, scaleX, scaleY)
        // Colon is built into the background panel at ~21-25
        // Digit 3 (Second tens) at startX + 27
        drawDigit(canvas, numbersBitmap, s1, startX + (27f * scaleX), startY, scaleX, scaleY)
        // Digit 4 (Second ones) at startX + 39
        drawDigit(canvas, numbersBitmap, s2, startX + (39f * scaleX), startY, scaleX, scaleY)
    }

    private fun drawDigit(
        canvas: Canvas,
        bmp: Bitmap,
        digit: Int,
        x: Float,
        y: Float,
        scaleX: Float,
        scaleY: Float
    ) {
        val srcX = (digit.coerceIn(0, 9)) * DIGIT_WIDTH
        val srcRect = Rect(srcX, 0, srcX + DIGIT_WIDTH, DIGIT_HEIGHT)
        val dstRect = RectF(
            x,
            y,
            x + (DIGIT_WIDTH * scaleX),
            y + (DIGIT_HEIGHT * scaleY)
        )
        canvas.drawBitmap(bmp, srcRect, dstRect, null)
    }
}
